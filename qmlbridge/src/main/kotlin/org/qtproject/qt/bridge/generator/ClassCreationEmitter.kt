/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import org.qtproject.qt.bridge.generator.*

internal class ClassCreationEmitter(private val codeGenerator: CodeGenerator) {

    // Functions for mapping variable types and shapes into bytes.
    // Use byte encoding (instead of Java/Kotlin side structs) so that
    // we can easily pass them to JNI, without JNI needing to invoke
    // Java/Kotlin functions to determine these shapes / types. Also
    // we want to avoid runtime-heavy parsing of method and parameter
    // signatures, and do the heavylifthing at build-time (at the expense
    // of few extra bytes of memory).
    // Must be kept in synch with JNI converter enum
    private fun VariableShape.code(): Byte = when (this) {
        VariableShape.VALUE -> 0
        VariableShape.LIST -> 1
        VariableShape.ARRAY -> 2
        VariableShape.MAP -> 3
    }.toByte();

    // Must be kept in synch with JNI-side converter enum and
    // ClassModel VariableType enum
    private fun VariableType.code(): Byte = when (this) {
        VariableType.VOID -> 0
        VariableType.BOOLEAN -> 1
        VariableType.BYTE -> 2
        VariableType.CHAR -> 3
        VariableType.SHORT -> 4
        VariableType.INT -> 5
        VariableType.LONG -> 6
        VariableType.FLOAT -> 7
        VariableType.DOUBLE -> 8
        VariableType.STRING -> 9
        VariableType.QML_REGISTRABLE -> 10
        VariableType.ITEM_MODEL -> 11
        // Note: from 128 (0x80) onwards the values are reserved for marking type's
        // Java-side representation as primitive/unboxed (the upmost bit is set)
    }.toByte();

    // Packs the 'primitive'/'unboxed' information into the high bit of type code
    private fun packedVariableTypeCode(type: VariableType, isPrimitive: Boolean): Byte {
        val base = type.code().toInt() and 0x7F
        val primitiveBit = if (isPrimitive) 0x80 else 0
        return (base or primitiveBit).toByte()
    }

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
            w.appendLine("package org.qtproject.qt.bridge.core")
            w.appendLine()

            if (packageName.isNotBlank())
                w.appendLine("import $packageName.$className")
            else
                w.appendLine("import $className")
            w.appendLine()

            w.appendLine("internal class $qtMetaName {")
            w.appendLine("    companion object {")
            w.appendLine("        @JvmStatic")
            w.appendLine("        // Registers both properties' change signals and @QMLSignals callback signals")
            w.appendLine("        fun registerSignals(qtObject : QtObject) {")
            if (model.signalField?.signals != null) {
                model.signalField.signals.forEach { sig ->
                    val cppParamTypes = sig.cppParams.map { it.second }
                    val cppParamTypesArray =
                        if (cppParamTypes.isEmpty()) "emptyArray<String>()"
                        else "arrayOf(" + cppParamTypes.joinToString(", ") { "\"$it\"" } + ")"

                    // Array, List, Map, ..
                    val paramShape = if (sig.paramListInfo.isEmpty()) "byteArrayOf()" else
                        "byteArrayOf(" + sig.paramListInfo.joinToString(", ") { it.shape.code().toString() } + ")"

                    // Integer, String, ... and value primitiveness bit
                    val paramType = if (sig.paramListInfo.isEmpty()) "byteArrayOf()" else
                        "byteArrayOf(" + sig.paramListInfo.joinToString(", ") {
                            packedVariableTypeCode(it.type, it.isPrimitive).toString()
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
                    "byteArrayOf(" + m.paramListInfo.joinToString(", ") { it.shape.code().toString() } + ")"

                // Integer, String, ... and value primitiveness bit
                val paramType = if (m.paramListInfo.isEmpty()) "byteArrayOf()" else
                    "byteArrayOf(" + m.paramListInfo.joinToString(", ") {
                        packedVariableTypeCode(it.type, it.isPrimitive).toString()
                    } + ")"

                w.appendLine(
                    "            qtObject.addInvokable(\n" +
                    "                \"${m.javaSignature}\", \"${m.javaReturnType}\",\n" +
                    "                \"${m.cppSignature}\", \"${m.cppReturnType}\",\n" +
                    "                ${m.retInfo.shape.code().toString()},\n" +
                    "                ${packedVariableTypeCode(m.retInfo.type, m.retInfo.isPrimitive).toString()},\n" +
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
                    "                ${typeInfo.shape.code()},\n" +
                    "                ${packedVariableTypeCode(typeInfo.type, typeInfo.isPrimitive)}\n" +
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
            w.appendLine("            val userObject = $className()")
            w.appendLine("            val userClass = $className::class.java")

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
    }

    // Generates the bootstrap function to initiate QML type registration
    fun emitQmlTypeRegistrationEntryPoint(qmlRegistrables: List<RegistrableClass>) {
        val packageName = "org.qtproject.qt.bridge.core"
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
