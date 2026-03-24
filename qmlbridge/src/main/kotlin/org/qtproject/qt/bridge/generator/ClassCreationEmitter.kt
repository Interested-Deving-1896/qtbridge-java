/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

internal class ClassCreationEmitter(private val codeGenerator: CodeGenerator) {

    val corePackage: String = "org.qtproject.qt.bridge.core"

    fun emitClassFromModel(model: RegistrableClass) {
        val packageName = model.packageName
        val className = model.simpleName
        val qtMetaName = "${className}_QtMeta"

        val file = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            packageName = packageName,
            fileName = qtMetaName
        )

        file.bufferedWriter().use { w ->
            w.appendLine("package ${corePackage}")
            w.appendLine()

            if (packageName.isNotBlank())
                w.appendLine("import $packageName.$className")
            else
                w.appendLine("import $className")

            if (model.signalMethods.isNotEmpty() && packageName.isNotBlank())
                w.appendLine("import $packageName.${className}_QtImpl")
            w.appendLine()

            w.appendLine("internal class $qtMetaName {")
            w.appendLine("    companion object {")
            w.appendLine("        @JvmStatic")
            w.appendLine("        // Registers all property change signals,")
            w.appendLine("        // @QMLSignals field signals, and individual @QMLSignal methods")
            w.appendLine("        fun registerSignals(qtObject : QtObject) {")
            val allSignals = (model.signalField?.signals.orEmpty()) + model.signalMethods
            allSignals.forEach { sig ->
                val cppParamTypes = sig.cppParams.map { it.second }
                val cppParamTypesArray =
                    if (cppParamTypes.isEmpty()) "emptyArray<String>()"
                    else "arrayOf(" + cppParamTypes.joinToString(", ") { "\"$it\"" } + ")"

                // Array, List, Map, ..
                val paramShape = if (sig.paramListInfo.isEmpty()) "byteArrayOf()" else
                    "byteArrayOf(" + sig.paramListInfo.joinToString(", ") { it.shape.code.toString() } + ")"

                // Integer, String, ... and value primitiveness bit
                val paramType = if (sig.paramListInfo.isEmpty()) "byteArrayOf()" else
                    "byteArrayOf(" + sig.paramListInfo.joinToString(", ") {
                        it.type.packedCode(it.isPrimitive).toString()
                    } + ")"

                w.appendLine(
                    "            qtObject.addSignal(\n" +
                    "                \"${sig.javaSignature}\",\n" +
                    "                \"${sig.cppSignature}\",\n" +
                    "                $cppParamTypesArray,\n" +
                    "                $paramShape,\n" +
                    "                $paramType)"
                )
            }
            // Add change signals to QtProperties (currently always no-arg)
            model.properties.forEach { p ->
                if (p.kind != PropertyKind.QT_PROPERTY) return@forEach
                val sig = p.notifySignalSignature
                // No arguments -> Java and C++ signatures are identical
                w.appendLine(
                    "            qtObject.addSignal(" +
                    "\"$sig\", \"$sig\", emptyArray<String>(), byteArrayOf(), byteArrayOf())"
                )
            }
            w.appendLine("        }")
            w.appendLine()

            w.appendLine("        // Register functions that are invokable from QML")
            w.appendLine("        @JvmStatic")
            w.appendLine("        fun registerInvokables(qtObject : QtObject) {")
            model.invokables.forEach { m ->

                // Array, List, Map, ..
                val paramShape = if (m.paramListInfo.isEmpty()) "byteArrayOf()" else
                    "byteArrayOf(" + m.paramListInfo.joinToString(", ") { it.shape.code.toString() } + ")"

                // Integer, String, ... and value primitiveness bit
                val paramType = if (m.paramListInfo.isEmpty()) "byteArrayOf()" else
                    "byteArrayOf(" + m.paramListInfo.joinToString(", ") {
                        it.type.packedCode(it.isPrimitive).toString()
                    } + ")"

                w.appendLine(
                    "            qtObject.addInvokable(\n" +
                    "                \"${m.javaSignature}\", \"${m.javaReturnType}\",\n" +
                    "                \"${m.cppSignature}\", \"${m.cppReturnType}\",\n" +
                    "                ${m.retInfo.shape.code.toString()},\n" +
                    "                ${m.retInfo.type.packedCode(m.retInfo.isPrimitive).toString()},\n" +
                    "                $paramShape, $paramType)"
                )
            }
            w.appendLine("        }")
            w.appendLine()

            w.appendLine("        // Register QtProperties (note that signals must be registered first)")
            w.appendLine("        @JvmStatic")
            w.appendLine("        fun registerProperties(qtObject : QtObject, userObject : $className) {")
            model.properties.forEach { p ->
                val type = p.type
                val declared = p.declaredTypeQualifiedName
                val typeInfo = p.typeInfo
                w.appendLine(
                    "            qtObject.addProperty(QProperty(\n" +
                    "                \"${p.name}\", \"$declared\",\n" +
                    "                \"${type.cppType}\", ${p.writableFromQml}, true,\n" +
                    "                ${p.constant}, \"${p.notifySignalSignature}\",\n" +
                    "                ${typeInfo.shape.code},\n" +
                    "                ${typeInfo.type.packedCode(typeInfo.isPrimitive)}\n" +
                    "            ))")
            }

            // connectPropertyNotifyToSignalEmission for QtProperty members
            model.properties.forEach { p ->
                if (p.kind != PropertyKind.QT_PROPERTY)
                    return@forEach
                val propName = p.name
                val escaped = "`$propName`"
                w.appendLine("            qtObject.connectPropertyNotifyToSignalEmission(\"$propName\", userObject.$escaped)")
            }

            model.signalField?.let { signalField ->
                w.appendLine("            // Inject QML signals proxy so user code can emit wrapper signals via @QMLSignals")
                w.appendLine("            try {")
                w.appendLine("                val signalField = $className::class.java.getDeclaredField(\"${signalField.fieldName}\")")
                w.appendLine("                signalField.isAccessible = true")
                w.appendLine("                val proxy = QtSignalProxy.bind(${signalField.interfaceQualifiedName}::class.java, qtObject)")
                w.appendLine("                signalField.set(userObject, proxy)")
                w.appendLine("            } catch (_: Throwable) {")
                w.appendLine("                // field missing or inaccessible -> ignore")
                w.appendLine("            }")
            }

            if (model.signalMethods.isNotEmpty()) {
                w.appendLine("            // Inject signal emitter so that the @QMLSignal overrides can emit signals")
                w.appendLine("            (userObject as? ${className}_QtImpl)?._qtSignalEmitter = qtObject")
            }

            w.appendLine("        }")
            w.appendLine()

            w.appendLine("        @JvmStatic")
            w.appendLine("        fun registerMeta(qtObject: QtObject, userObject : Any) {")
            w.appendLine("            val typedUserObject : $className = userObject as $className")
            w.appendLine("            registerSignals(qtObject)")
            w.appendLine("            registerInvokables(qtObject)")
            w.appendLine("            registerProperties(qtObject, typedUserObject)")
            w.appendLine("        }")
            w.appendLine()

            w.appendLine("        @JvmStatic")
            w.appendLine("        fun registerAsQmlType() {")
            // When @QMLSignal methods exist, instantiate and register _QtImpl (the generated
            // concrete subclass) — the user's class may be abstract and cannot be instantiated directly.
            val implClassName = if (model.signalMethods.isNotEmpty()) "${className}_QtImpl" else className
            w.appendLine("            val userObject = $implClassName()")
            w.appendLine("            val userClass = $implClassName::class.java")

            val reg = model.registrableInfo
            val qmlTypeName = reg?.typeName ?: className
            val moduleName = reg?.moduleName ?: "QtBridge"
            val singleton = reg?.isSingleton ?: false

            w.appendLine("            QtQmlRegistration.registerQmlType(")
            w.appendLine("                \"$qmlTypeName\"")
            w.appendLine("                , \"$moduleName\"")
            w.appendLine("                , $singleton")
            w.appendLine("                , ${model.qmlCompleteHandlerName?.let { "\"$it\"" } ?: "null"}")
            w.appendLine("                , userClass")
            w.appendLine("            )")
            w.appendLine("        }")

            w.appendLine("    }")
            w.appendLine("}")
        }

        if (model.signalMethods.isNotEmpty())
            emitQtImplClass(model)
    }

    // Emits a generated internal override ('_QtImpl') of the user class for
    // overriding signal methods marked with @QMLSignal. The class is generated
    // in the user class's package, and uses signal emitter from core package
    private fun emitQtImplClass(model: RegistrableClass) {
        val userPackageName = model.packageName
        val className = model.simpleName
        val implName = "${className}_QtImpl"
        val file = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            packageName = userPackageName,
            fileName = implName
        )

        file.bufferedWriter().use { w ->
            w.appendLine("package $userPackageName")
            w.appendLine()
            w.appendLine("import ${corePackage}.QtSignalEmitter")
            w.appendLine()
            w.appendLine("internal class $implName : $className() {")
            w.appendLine("    internal var _qtSignalEmitter: QtSignalEmitter? = null")
            // Emit overrides for each signal method (@QMLSignal)
            model.signalMethods.forEach { sig ->
                val methodName = sig.javaSignature.substringBefore("(")
                val overrideParams = sig.methodOverrideParams!!
                val paramList = overrideParams.joinToString(", ") {
                    (name, type) -> "$name: $type"
                }
                val argList = overrideParams.joinToString(", ") {
                    (name, _) -> name
                }
                val emitArgs = if (argList.isEmpty()) "" else ", $argList"
                w.appendLine()
                w.appendLine("    override fun $methodName($paramList) {")
                w.appendLine("        _qtSignalEmitter?.emitSignal(\"${sig.javaSignature}\"$emitArgs)")
                w.appendLine("    }")
            }
            w.appendLine("}")
        }

        // QtObject.initializeMeta() looks up "*_QtMeta". When userClass is *_QtImpl,
        // it, too, needs a *_QtMeta file. Generate that as a thin delegating class that
        // forwards the registerMeta() to the actual base class's _QtMeta (which then
        // registers the actual user signals, properties, and invokables).
        val implMetaName = "${implName}_QtMeta"
        val implMetaFile = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            packageName = "$corePackage",
            fileName = implMetaName
        )
        implMetaFile.bufferedWriter().use { w ->
            w.appendLine("package $corePackage")
            w.appendLine()
            w.appendLine("internal class $implMetaName {")
            w.appendLine("    companion object {")
            w.appendLine("        @JvmStatic")
            w.appendLine("        fun registerMeta(qtObject: QtObject, userObject: Any) {")
            w.appendLine("            ${className}_QtMeta.registerMeta(qtObject, userObject)")
            w.appendLine("        }")
            w.appendLine("    }")
            w.appendLine("}")
        }
    }

    // Generates the bootstrap function to initiate QML type registration
    fun emitQmlTypeRegistrationEntryPoint(qmlRegistrables: List<RegistrableClass>) {
        val packageName = "$corePackage"
        val fileName = "QtQmlRegistration_QtMeta"
        val file = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            packageName,
            fileName
        )
        file.bufferedWriter().use { writer ->
            writer.apply {
                appendLine("package $packageName")
                appendLine()
                appendLine("object $fileName {")
                appendLine("    @JvmStatic")
                appendLine("    fun registerRegistrablesAsQmlTypes() {")
                qmlRegistrables
                    .map{ it.simpleName }
                    .distinct()
                    .sorted()
                    .forEach { simpleName ->
                        appendLine("        ${simpleName}_QtMeta.registerAsQmlType()")
                    }
                appendLine("    }")
                appendLine("}")
            }
        }
    }
}
