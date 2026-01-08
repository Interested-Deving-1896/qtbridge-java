/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils

import org.qtproject.qt.bridge.core.QtListModel

import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSType

internal data class JvmType(val javaType: String, val cppType: String, val isPrimitive: Boolean = false)
{
    private data class Mapping(
        val javaType: String,
        val cppType: String,
        val isPrimitive: Boolean = false,
    )

    companion object {
        private val kotlinTypeToJavaAndCppTypeMap = mapOf(
            // Kotlin primitives and wrappers => Java
            "kotlin.String" to Mapping("java.lang.String", "QString"),
            "kotlin.Int" to Mapping("java.lang.Integer", "int"),
            "kotlin.Long" to Mapping("java.lang.Long", "qint64"), // qint64 == long long ~ jlong
            "kotlin.Short" to Mapping("java.lang.Short", "short"),
            "kotlin.Byte" to Mapping("java.lang.Byte", "qint8"), // // qint8 == signed char == jbyte
            "kotlin.Char" to Mapping("java.lang.Character", "QChar"), // Java char is UTF-16
            "kotlin.Boolean" to Mapping("java.lang.Boolean", "bool"),
            "kotlin.Float" to Mapping("java.lang.Float", "float"),
            "kotlin.Double" to Mapping("java.lang.Double", "double"),

            "kotlin.collections.List" to Mapping("java.util.List", "QVariantList"),
            "kotlin.collections.MutableList" to Mapping("java.util.List", "QVariantList"),
            "kotlin.collections.Map" to Mapping("java.util.Map", "QVariantMap"),
            "kotlin.collections.MutableMap" to Mapping("java.util.Map", "QVariantMap"),
            QtListModel::class.qualifiedName!! to Mapping(QtListModel::class.qualifiedName!!, "QAbstractItemModel*"),
            "java.net.URI" to Mapping("java.net.URI", "QUrl"),
        )

        private fun mapKotlinTypeToJavaAndCppType(
            rawName: String,  type: KSType,  builtIns: KSBuiltIns): Mapping? {
            // Check if we can map the list to QStringList. This is an optimization as QML
            // has direct support for QStringList; strings would work as QVariantList too
            if (rawName == "kotlin.collections.List"
                || rawName == "kotlin.collections.MutableList"
                || rawName == "java.util.List") {
                val argType = type.arguments.singleOrNull()?.type?.resolve()
                val isStringArg = argType != null && (
                    argType.makeNotNullable() == builtIns.stringType
                         || argType.declaration.qualifiedName?.asString() in setOf("kotlin.String", "java.lang.String"))
                return if (isStringArg) {
                    Mapping("java.util.List", "QStringList")
                } else {
                    Mapping("java.util.List", "QVariantList")
                }
            }
            return kotlinTypeToJavaAndCppTypeMap[rawName]
        }

        fun fromKSType(type: KSType?, builtIns: KSBuiltIns, logger: KSPLogger, varName: String = ""): JvmType {
            if (type == null)
                return JvmType("void", "void")

            return when (type) {
                builtIns.unitType -> JvmType("void", "void", true)
                builtIns.intType -> JvmType("int", "int", true)
                builtIns.longType -> JvmType("long", "qint64", true) // qint64 == long long ~ jlong
                builtIns.shortType -> JvmType("short", "short", true)
                builtIns.byteType -> JvmType("byte", "qint8", true)  // qint8 == signed char == jbyte
                builtIns.charType -> JvmType("char", "QChar", true) // // Java char is UTF-16
                builtIns.booleanType -> JvmType("boolean", "bool", true)
                builtIns.floatType -> JvmType("float", "float", true)
                builtIns.doubleType -> JvmType("double", "double", true)

                else -> {
                    val decl = type.declaration as? KSClassDeclaration
                    val rawName = if (decl != null) {
                        // Build JVM binary name: package$nestedName. These are types that
                        // are declared nested in another class (for example an enum class)
                        val pkg = decl.packageName.asString()
                        val segments = mutableListOf<String>()
                        var current: KSClassDeclaration? = decl
                        while (current != null) {
                            segments += current.simpleName.asString()
                            current = current.parentDeclaration as? KSClassDeclaration
                        }
                        segments.reverse()
                        val simpleBinary = segments.joinToString("\\$")
                        if (pkg.isNotEmpty()) "$pkg.$simpleBinary" else simpleBinary
                    } else {
                        type.declaration.qualifiedName?.asString() ?: type.toString()
                    }

                    // Map the raw name to Java and cpp types
                    val mapped = mapKotlinTypeToJavaAndCppType(rawName, type, builtIns)
                    //logger.warn("JvmType.fromKSType() mapped $rawName to $mapped")
                    if (mapped != null)
                        return JvmType(mapped.javaType, mapped.cppType)

                    // No predefined type matched. The type may still legitimately be QMLRegistrable or an enum
                    if (type.isEnum())
                        return JvmType(rawName, "QVariantMap")
                    if (type.isQmlRegistrable())
                        return JvmType(rawName, "QObject*")
                    // There shouldn't be a supported use case where no mapping was found.
                    // Let's properly catch the cases that are not currently supported
                    logger.error("KSP type $rawName not recognized ($varName), things may not work properly")
                    JvmType(rawName, "QVariant")
                }
            }
        }
    }
}
