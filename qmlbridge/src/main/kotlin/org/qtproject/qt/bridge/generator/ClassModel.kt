/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.generator

import org.qtproject.qt.bridge.utils.JvmType

internal enum class PropertyKind {
    QT_PROPERTY,
    QT_LIST_MODEL,
}

// Keep numeric values in sync with C++ Utility::JNI::VarShape.
internal enum class VariableShape(val code: Byte) {
    VALUE(0),
    LIST(1),
    ARRAY(2),
    MAP(3),
}

// Keep numeric values in sync with C++ Utility::JNI::VarType.
// Use byte encoding for shapes and types so that we can easily pass
// them to JNI, without JNI needing to invoke Java/Kotlin functions
// to determine these shapes / types. Also we want to avoid runtime
// JNI parsing of method and parameter signatures (for performance).
internal enum class VariableType(val code: Byte) {
    VOID(0),
    BOOLEAN(1),
    BYTE(2),
    CHAR(3),
    SHORT(4),
    INT(5),
    LONG(6),
    FLOAT(7),
    DOUBLE(8),
    STRING(9),
    QML_REGISTRABLE(10),
    ITEM_MODEL(11),
    // Values starting from 128 / 0x80 are reserved, see packedCode() below
    ;

    // Packs primitive/unboxed information into the high bit of the encoded value.
    fun packedCode(isPrimitive: Boolean): Byte {
        val base = code.toInt() and 0x7F
        val primitiveBit = if (isPrimitive) 0x80 else 0
        return (base or primitiveBit).toByte()
    }
}

// Stores information of function parameters, return values and property types.
// This information is needed for JNI to know which type Java/Kotlin expects
// (consider for example QML writing a QVariantList to a property).
internal data class VariableInfo(
   val shape: VariableShape,
   val type: VariableType,
   val isPrimitive: Boolean
)

// Source location data for MOC output
internal data class SourceLocation(
    val fileName: String,
    val filePath: String?,
    val lineNumber: Int?, // 1-based
)

// Annotation data provided by @QMLRegistrable annotation itself
internal data class RegistrableInfo(
    val typeName: String,
    val moduleName: String,
    val isSingleton: Boolean,
    val includeSuper: Boolean,
)

// Models @QMLRegistrable
internal data class RegistrableClass(
    val cppType: String,
    val packageName: String,
    val simpleName: String,
    val qualifiedName: String,
    val registrableSuperClass: String? = null, // Nearest @QMLRegistrable in hierarchy
    val registrableInfo: RegistrableInfo? = null,
    val sourceLocation: SourceLocation? = null,
    val invokables: List<Invokable> = emptyList(),
    val properties: List<Property> = emptyList(),
    // @QMLSignals field
    val signalField: SignalField? = null,
    // @QMLComplete handler (just name, signature is known)
    val qmlCompleteHandlerName: String? = null,
)

// Models public methods within a @QMLRegistrable
internal data class Invokable(
    val name: String,
    val javaSignature: String, // "foo(java.lang.Integer,java.lang.String)"
    val cppSignature: String, // "foo(int,QString)"
    val cppParams: List<Pair<String, String>>, // 'name, type' pairs
    val javaReturnType: String, // "void", "int", "java.lang.String"
    val cppReturnType: String, // "void", "int", "QString"
    val retInfo: VariableInfo,
    val paramListInfo: List<VariableInfo>,
    val sourceLocation: SourceLocation? = null,
)

// Models a signal within a @QMLSignals field
internal data class Signal(
    val javaSignature: String,
    val cppSignature: String,
    val cppParams: List<Pair<String, String>>, // 'name, type' pairs
    val paramListInfo: List<VariableInfo>,
    val sourceLocation: SourceLocation? = null,
)

// @QMLSignals field
internal data class SignalField(
    val fieldName: String,
    val interfaceQualifiedName: String,
    val signals: List<Signal> = emptyList(),
)

// Models QtProperty<T> or a QtListModel<T> member of a @QMLRegistrable
internal data class Property(
    val kind: PropertyKind,
    val name: String,
    val notifySignalSignature: String, // e.g. "textChanged()"
    val constant: Boolean,
    val writableFromQml: Boolean,
    val type: JvmType,
    val typeInfo: VariableInfo,
    val declaredTypeQualifiedName: String?,
    val sourceLocation: SourceLocation?,
)
