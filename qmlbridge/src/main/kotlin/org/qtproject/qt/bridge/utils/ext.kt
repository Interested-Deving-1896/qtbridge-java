/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils

import org.qtproject.qt.bridge.annotations.QMLRegistrable

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.ClassKind
import kotlin.reflect.KClass

// Helper for determining if a type is QMLRegistrable
internal fun KSType.isQmlRegistrable(): Boolean {
    val cls = declaration as? KSClassDeclaration ?: return false
    return cls.annotations.any {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == QMLRegistrable::class.qualifiedName
    }
}

// Helper for determining if a type is an Enum
internal fun KSType.isEnum(): Boolean {
    val cls = declaration as? KSClassDeclaration ?: return false
    if (cls.classKind == ClassKind.ENUM_CLASS) return true
    val qName = cls.qualifiedName?.asString()
    if (qName == "java.lang.Enum") return true
    // Check if extends java.lang.Enum
    return cls.superTypes.any {
        it.resolve().declaration.qualifiedName?.asString() == "java.lang.Enum"
    }
}

internal fun KSClassDeclaration.extends(baseName: String, resolver: Resolver): Boolean {
    return this.superTypes.any {
        val resolved = it.resolve()
        resolved.declaration.qualifiedName?.asString() == baseName ||
                (resolved.declaration as? KSClassDeclaration)?.extends(baseName, resolver) == true
    }
}

internal fun KSFunctionDeclaration.hasAnnotation(annotation: KClass<*>): Boolean {
    return annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == annotation.qualifiedName }
}

internal fun KSPropertyDeclaration.hasAnnotation(annotation: KClass<*>): Boolean {
    return annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == annotation.qualifiedName }
}

internal fun KSPropertyDeclaration.findAnnotation(annotation: KClass<*>): KSAnnotation? {
    return annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == annotation.qualifiedName }
}

internal inline fun <reified T> KSAnnotation.getArgument(name: String): T? {
    return arguments.find { it.name?.asString() == name }?.value as? T
}

// Finds the nearest enclosing class declaration for the node (annotated symbol).
// Returns null if not found.
internal fun KSNode.containingClass(): KSClassDeclaration? {
    var current: KSNode? = this
    while (current != null) {
        if (current is KSClassDeclaration)
            return current
        current = current.parent
    }
    return null
}

internal fun KSType.isType(simpleName: String): Boolean {
    return declaration.simpleName.asString() == simpleName
}

internal fun KSType.isQtProperty() =  isType("QtProperty")

internal fun KSType.isQtListModel() = isType("QtListModel")
