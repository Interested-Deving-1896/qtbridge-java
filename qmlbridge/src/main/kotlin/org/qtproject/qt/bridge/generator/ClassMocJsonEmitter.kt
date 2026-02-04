/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import org.json.JSONArray
import org.json.JSONObject

internal class ClassMocJsonEmitter(private val codeGenerator: CodeGenerator) {
    private companion object {
        // MOC's hardcoded revision number
        const val MOC_OUTPUT_REVISION = 69
        // Default module name if user provided none
        private const val DEFAULT_QML_MODULE_NAME = "QtBridge"
        // Pretty-printed JSON indentation
        private const val INDENTATION_DEPTH = 4
    }

    // Main entry point function. Takes in a list of @QMLRegistrable classes and
    // emits a <module>_moc.json file for each QML module. The output mimics
    // Qt MOC JSON output, which is intended as an input for qmltyperegistrar
    fun emitMocJsonPerModule(qmlRegistrables: List<RegistrableClass>) {
        // Group QMLRegistrable classes into their QML modules so that we produce
        // one json file for each module
        val byModule = qmlRegistrables.groupBy { it.registrableInfo?.moduleName ?: DEFAULT_QML_MODULE_NAME }
        byModule.forEach { (moduleName, classes) ->
            emitModuleMocJsonFile(moduleName, classes)
        }
    }

    // Emits a single <module>_moc.json file
    private fun emitModuleMocJsonFile(moduleName: String, classes: List<RegistrableClass>) {
        // Group classes according to their source location file
        val bySourceFile = classes.groupBy {
            it.sourceLocation?.filePath
                ?: it.sourceLocation?.fileName
                ?: "${it.simpleName}.kt"
        }

        // MOC JSON root element is an array which contains objects per source file,
        // which in turn then contain the classes in an array
        //[
        //    {
        //        "inputFile": "Animals.java",
        //        "outputRevision": 69
        //        "classes": [{...}, {...}]
        //    },
        //    {
        //        "inputFile": "Vehicles.java",
        //        ...
        //    },
        //    ...
        //]
        val rootArray = JSONArray()

        bySourceFile.forEach { (sourceFilePath, fileClasses) ->
            val sourceFileObject = JSONObject()
                .put("inputFile", sourceFilePath)
                .put("outputRevision", MOC_OUTPUT_REVISION)
                .put("classes", buildClassesArray(fileClasses))

            rootArray.put(sourceFileObject)
        }

        val fileBaseName = fileNameFromModuleName(moduleName) + "_moc"
        val file = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            packageName = "",
            fileName = fileBaseName,
            extensionName = "json"
        )

        // Write the JSON file, use pretty print to ease debugging
        file.bufferedWriter().use { it.write(rootArray.toString(INDENTATION_DEPTH)) }
    }

    // Builds a JSONArray which contains QML element definitions
    private fun buildClassesArray(classes: List<RegistrableClass>): JSONArray {
        val arr = JSONArray()
        classes.forEach { model ->
            arr.put(buildClassObject(model))
        }
        return arr
    }

    // Single class object corresponds to a QML element defined in Java
    // {
    // "classInfos": [
    //     {
    //         "name": "QML.Element",
    //         "value": "auto"
    //     },
    //     ...
    // ],
    // "className": "Cat",
    // ...
    // "properties": [
    //     {
    //         "constant": false,
    //          ...
    private fun buildClassObject(model: RegistrableClass): JSONObject {
        val classObj = JSONObject()

        classObj.put("classInfos", buildClassInfosArray(model))
        classObj.put("className", model.simpleName)

        // Currently using full qualified com.example.MyType name would
        // assert qmltyperegistar as it expects C++ namespacing with ::
        // => use simple name instead
        classObj.put("qualifiedClassName", model.simpleName)
        // obj.put("qualifiedClassName", model.qualifiedName)

        model.sourceLocation?.lineNumber?.let { classObj.put("lineNumber", it) }

        classObj.put("object", true)
        classObj.put("gadget", false)
        classObj.put("namespace", false)

        classObj.put("properties", buildPropertiesArray(model.properties))
        classObj.put("methods", buildMethodsArray(model.invokables))
        classObj.put("signals", buildSignalsArray(model))

        // Empty arrays, currently nothing to put in them
        // We don't have notion of slots, and invokables are handled by 'methods' above
        classObj.put("slots", JSONArray())
        classObj.put("constructors", JSONArray())
        classObj.put("enums", JSONArray())
        classObj.put("interfaces", JSONArray())

        // Superclass of the QML element; either the nearest QMLRegistrable
        // parent, or one of C++ classes we support (QObject / QAbstractListModel)
        val superClassName = model.registrableSuperClass ?: model.cppType
        val superClasses = JSONArray().put(
            JSONObject()
                .put("access", "public")
                .put("name", superClassName)
        )
        classObj.put("superClasses", superClasses)

        return classObj
    }

    // Classinfos is an array of objects describing how a type should be
    // registered in the QML type system
    // "classInfos": [
    //     {
    //        "name": "QML.Element",
    //        "value": "myGadget"
    //      ...
    //
    // Currently the only supported ones are QML.Element and QML.Singleton
    private fun buildClassInfosArray(model: RegistrableClass): JSONArray {
        val classInfosArray = JSONArray()

        val qmlElementValue = when {
            model.registrableInfo == null -> "auto"
            model.registrableInfo.typeName == model.simpleName -> "auto"
            else -> model.registrableInfo.typeName
        }

        classInfosArray.put(
            JSONObject()
                .put("name", "QML.Element")
                .put("value", qmlElementValue)
        )

        if (model.registrableInfo?.isSingleton == true) {
            classInfosArray.put(
                JSONObject()
                    .put("name", "QML.Singleton")
                    .put("value", "true")
            )
        }

        classInfosArray.put(
            JSONObject()
                .put("name", "DefaultProperty")
                .put("value", "children")
        )

        return classInfosArray
    }

    // Properties array contains the QtProperties as objects
    // "properties": [
    //     {
    //         "name": "myProperty"
    // ...
    private fun buildPropertiesArray(properties: List<Property>): JSONArray {
        val propertiesArray = JSONArray()
        properties.forEachIndexed { index, property ->
            val propertyObj = JSONObject()
                .put("name", property.name)
                .put("constant", property.constant)
                .put("designable", true)
                .put("final", false)
                .put("index", index) // see comment on methods array indexes
                .put("member", property.name)
                .put("required", false)
                .put("scriptable", true)
                .put("stored", true)
                .put("type", property.type.cppType)
                .put("user", false)

            property.sourceLocation?.lineNumber?.let { propertyObj.put("lineNumber", it) }

            if (property.kind == PropertyKind.QT_PROPERTY)
                propertyObj.put("notify", property.notifySignalSignature.removeSuffix("()"))

            propertiesArray.put(propertyObj)
        }

        val childrenPropertyObj = JSONObject()
            .put("name", "children")
            .put("constant", true)
            .put("designable", true)
            .put("final", false)
            .put("index", properties.size)
            .put("read", "children")
            .put("required", false)
            .put("scriptable", true)
            .put("stored", false)
            .put("type", "QQmlListProperty<QObject>")
            .put("user", false)

        propertiesArray.put(childrenPropertyObj)
        return propertiesArray
    }

    // Methods array contains the invokable methods (public QMLRegistrable methods) as objects
    // "methods": [
    // {
    //    "name": "doSomething",
    //    "arguments": [
    //    ...
    private fun buildMethodsArray(invokables: List<Invokable>): JSONArray {
        val methodsArray = JSONArray()

        invokables.forEachIndexed { index, invokable ->

            val methodArgumentsArray = JSONArray()
                invokable.cppParams.forEach { (name, type) ->
                methodArgumentsArray.put(JSONObject().put("name", name).put("type", type))
            }

            val methodObj = JSONObject()
                .put("name", invokable.name)
                .put("arguments", methodArgumentsArray)
                .put("access", "public")
                // QMetaObject index used in the tooling file. The exact value is not critical as long as it
                // is non-negative; these indexes are only relevant to the QML compiler, which is not used
                // by the Java bridge.
                .put("index", index)
                // QTBUG-143296: Kotlin functions may have default arguments. This might imply
                // we need to generate method with and without the parameter (isCloned true)
                .put("isCloned", false)
                .put("isConstructor", false)
                .put("isConst", false)
                .put("isJavaScriptFunction", false)
                .put("returnType", invokable.cppReturnType)

            invokable.sourceLocation?.lineNumber?.let { methodObj.put("lineNumber", it) }
            methodsArray.put(methodObj)
        }
        return methodsArray
    }

    // Signals array contains the invokable methods (public QMLRegistrable methods) as objects
    // "signals": [
    // {
    //    "name": "didSomething",
    //    "arguments": [
    //    ...
    private fun buildSignalsArray(model: RegistrableClass): JSONArray {
        val signalsArray = JSONArray()
        var idx = 0

        // Property notify signals (QtProperty)
        model.properties.forEach { property ->
            if (property.kind != PropertyKind.QT_PROPERTY) return@forEach
            val signalObj = JSONObject()
                .put("name", property.notifySignalSignature.removeSuffix("()"))
                .put("access", "public")
                .put("arguments", JSONArray()) // property notifiers don't have args
                .put("index", idx++) // see comment on methods array indexes
                .put("returnType", "void")

            property.sourceLocation?.lineNumber?.let { signalObj.put("lineNumber", it) }
            signalsArray.put(signalObj)
        }

        // @QMLSignals signals, which can have also arguments
        model.signalField?.signals?.forEach { signal ->
            val signalArgumentsArray = JSONArray()
            signal.cppParams.forEach { (name, type) ->
                signalArgumentsArray.put(JSONObject().put("name", name).put("type", type))
            }
            val signalObj = JSONObject()
                .put("access", "public")
                .put("arguments", signalArgumentsArray)
                .put("index", idx++) // see comment on methods array indexes
                .put("name", signal.cppSignature.substringBefore('('))
                .put("returnType", "void")

            signal.sourceLocation?.lineNumber?.let { signalObj.put("lineNumber", it) }
            signalsArray.put(signalObj)
        }

        return signalsArray
    }

    // Derive file base name from module name. This resulting filename part
    // is used later to decide the directory of the module files (qmldir etc).
    // For example 'my.module' files are put in 'my/module/'
    private fun fileNameFromModuleName(moduleName: String): String {
        val name = StringBuilder(moduleName.length)
        for (c in moduleName) {
            when {
                c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' -> name.append(c)
                c == '_' || c == '-' || c == '.' -> name.append(c)
                else -> name.append('_')
            }
        }
        return if (name.isEmpty()) DEFAULT_QML_MODULE_NAME else name.toString()
    }
}
