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

            "kotlin.IntArray" to Mapping("int[]", "QVariantList", isPrimitive = true),
            "kotlin.LongArray" to Mapping("long[]", "QVariantList", isPrimitive = true),
            "kotlin.ShortArray" to Mapping("short[]", "QVariantList", isPrimitive = true),
            "kotlin.FloatArray" to Mapping("float[]", "QVariantList", isPrimitive = true),
            "kotlin.DoubleArray" to Mapping("double[]", "QVariantList", isPrimitive = true),
            "kotlin.ByteArray" to Mapping("byte[]", "QVariantList", isPrimitive = true),
            "kotlin.CharArray" to Mapping("char[]", "QVariantList", isPrimitive = true),
            "kotlin.BooleanArray" to Mapping("boolean[]", "QVariantList", isPrimitive = true),

            "kotlin.collections.Map" to Mapping("java.util.Map", "QVariantMap"),
            "kotlin.collections.MutableMap" to Mapping("java.util.Map", "QVariantMap"),
            QtListModel::class.qualifiedName!! to Mapping(QtListModel::class.qualifiedName!!, "QAbstractItemModel*"),
            "java.net.URI" to Mapping("java.net.URI", "QUrl"),
        )

        private fun mapKotlinTypeToJavaAndCppType(rawName: String,  type: KSType,  builtIns: KSBuiltIns): Mapping?
        {
            // Check if a collections List (handled here instead of the mapping table so
            // we can handle List<String> as a special case
            if (rawName == "kotlin.collections.List" || rawName == "kotlin.collections.MutableList" || rawName == "java.util.List") {
                val argType = type.arguments.singleOrNull()?.type?.resolve()
                // Check if we can map the list to QStringList. This is an optimization as QML
                // has direct support for QStringList; strings would work as QVariantList too
                val isStringArg = argType != null && (
                    argType.makeNotNullable() == builtIns.stringType
                         || argType.declaration.qualifiedName?.asString() in setOf("kotlin.String", "java.lang.String"))
                return if (isStringArg) {
                    Mapping("java.util.List", "QStringList")
                } else {
                    Mapping("java.util.List", "QVariantList")
                }
            }

            // Check if a plain/raw array and verify it's one of the supported types
            if (rawName == "kotlin.Array") {
                val argType = type.arguments.singleOrNull()?.type?.resolve()?.makeNotNullable()
                if (argType != null) {
                    // String[] => QStringList (similar optimization as with collection Lists)
                    val isStringArg = (argType == builtIns.stringType
                        || argType.declaration.qualifiedName?.asString() in setOf("kotlin.String", "java.lang.String"))
                    if (isStringArg)
                        return Mapping("java.lang.String[]", "QStringList")

                    // Boxed primitive arrays => QVariantList
                    val qualifiedName = argType.declaration.qualifiedName?.asString()
                    return when (qualifiedName) {
                        "kotlin.Int", "java.lang.Integer" -> Mapping("java.lang.Integer[]", "QVariantList")
                        "kotlin.Long", "java.lang.Long" -> Mapping("java.lang.Long[]", "QVariantList")
                        "kotlin.Short", "java.lang.Short" -> Mapping("java.lang.Short[]", "QVariantList")
                        "kotlin.Byte", "java.lang.Byte" -> Mapping("java.lang.Byte[]", "QVariantList")
                        "kotlin.Char", "java.lang.Character" -> Mapping("java.lang.Character[]", "QVariantList")
                        "kotlin.Boolean", "java.lang.Boolean" -> Mapping("java.lang.Boolean[]", "QVariantList")
                        "kotlin.Float", "java.lang.Float" -> Mapping("java.lang.Float[]", "QVariantList")
                        "kotlin.Double", "java.lang.Double" -> Mapping("java.lang.Double[]", "QVariantList")
                        else -> null
                    }
                }
                return null
            }

            // Kotlin type is neither a Collection or an array containing boxed primitives,
            // use the mapping table
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
                        return JvmType(mapped.javaType, mapped.cppType, mapped.isPrimitive)

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
