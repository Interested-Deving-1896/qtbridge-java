/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import org.qtproject.qt.bridge.annotations.QMLComplete
import org.qtproject.qt.bridge.annotations.QMLRegistrable
import org.qtproject.qt.bridge.annotations.QMLSignals
import org.qtproject.qt.bridge.generator.*
import org.qtproject.qt.bridge.utils.*

internal class ClassModelGenerator(
    private val resolver: Resolver,
    private val logger: KSPLogger,
) {
    @Suppress("UNCHECKED_CAST")
    private fun <T> KSAnnotation.arg(name: String): T? =
        arguments.firstOrNull { it.name?.asString() == name }?.value as? T

    private fun KSType.qtPropertyValueTypeOrNull(): KSType? =
        if (isQtProperty()) arguments.firstOrNull()?.type?.resolve() else null

    private fun sourceLocationOf(node: KSNode): SourceLocation? {
        val loc = node.location as? FileLocation ?: return null
        val inputFile = java.io.File(loc.filePath)
        val inputFilePath = inputFile.absolutePath
        val inputFileName = inputFile.name
        return SourceLocation(
            fileName = inputFileName,
            filePath = inputFilePath,
            lineNumber = loc.lineNumber
        )
    }

    private fun KSClassDeclaration.QMLRegistrableInfo(): RegistrableInfo? {
        val qmlRegistrableQn = QMLRegistrable::class.qualifiedName ?: return null
        val ann = annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == qmlRegistrableQn
        } ?: return null

        val name = ann.arg<String>("name").orEmpty()
        val module = ann.arg<String>("module").orEmpty()
        val singleton = ann.arg<Boolean>("singleton") ?: false

        val typeName = name.ifBlank { simpleName.asString() }
        val moduleName = module.ifBlank { "QtBridge" }
        return RegistrableInfo(typeName, moduleName, singleton)
    }

    // Generates intermediate registrable class model, which is then used to
    // yield metaobjectbuilder calls and qmltyperegistrar input
    fun generate(klass: KSClassDeclaration): RegistrableClass {
        val pkg = klass.packageName.asString()
        val simple = klass.simpleName.asString()
        val qualified = klass.qualifiedName?.asString() ?: run {
            if (pkg.isNotEmpty()) "$pkg.$simple" else simple
        }

        val registrableInfo = klass.QMLRegistrableInfo()
        val invokables = buildInvokables(klass)
        val properties = buildProperties(klass)
        val signalField = buildSignals(klass)

        return RegistrableClass(
            cppType = "QObject", // Fixed atm, may change if we support visual types (QQuickItem)
            packageName = pkg,
            simpleName = simple,
            qualifiedName = qualified,
            registrableInfo = registrableInfo,
            sourceLocation = sourceLocationOf(klass),
            invokables = invokables,
            properties = properties,
            signalField = signalField,
            qmlCompleteHandlerName = findQmlCompleteHandlerName(klass),
        )
    }

    private fun buildInvokables(klass: KSClassDeclaration): List<Invokable> =
        klass.getDeclaredFunctions()
            .filter { it.isPublic() && !it.isConstructor() }
            .map { fn ->
                val returnJvm = JvmType.fromKSType(fn.returnType?.resolve(), resolver.builtIns, logger, "return: $fn")
                val paramJvmTypes = fn.parameters.map { p ->
                    JvmType.fromKSType(p.type.resolve(), resolver.builtIns, logger, "parameter: $p")
                }

                val javaSig = buildString {
                    append(fn.simpleName.asString())
                    append("(")
                    append(paramJvmTypes.joinToString(",") { it.javaType })
                    append(")")
                }
                val cppSig = buildString {
                    append(fn.simpleName.asString())
                    append("(")
                    append(paramJvmTypes.joinToString(",") { it.cppType })
                    append(")")
                }

                // List of 'name, type' arguments for the function
                val cppParams: List<Pair<String, String>> = fn.parameters.map { parameter ->
                    val jvm = JvmType.fromKSType(parameter.type.resolve(), resolver.builtIns, logger, "parameter: $parameter")
                    val name = parameter.name?.asString() ?: "arg"  // fallback to generic 'arg'
                    name to jvm.cppType
                }

                Invokable(
                    name = fn.simpleName.asString(),
                    javaSignature = javaSig,
                    cppSignature = cppSig,
                    cppParams = cppParams,
                    javaReturnType = returnJvm.javaType,
                    cppReturnType = returnJvm.cppType,
                    retIsPrimitive = returnJvm.isPrimitive,
                    paramIsPrimitive = paramJvmTypes.map { it.isPrimitive }.toBooleanArray(),
                    sourceLocation = sourceLocationOf(fn)
                )
            }
            .toList()

    private fun buildProperties(klass: KSClassDeclaration): List<Property> =
        klass.getDeclaredProperties().mapNotNull { prop ->
            val name = prop.simpleName.asString()
            val propType = prop.type.resolve()

            val sourceLocation = sourceLocationOf(prop)
            val notify = "${name}Changed()"

            val isQtProperty = propType.isQtProperty()
            val isQtListModel = propType.isQtListModel()
            if (isQtProperty || isQtListModel) {
                val valueType =
                    if (isQtProperty) propType.qtPropertyValueTypeOrNull()
                    else propType

                if (valueType == null) {
                    logger.error("Property $name type is null, cannot continue", prop)
                    return@mapNotNull null
                }

                var constant = false
                var writable = true
                if (isQtListModel) {
                    constant = true
                    writable = false
                } else if (valueType.isEnum()) {
                    writable = false // Enums are not writable(QTBUG-141710)
                }

                val declaredFq = (propType.declaration as? KSClassDeclaration)?.qualifiedName?.asString()
                val mapped = JvmType.fromKSType(valueType, resolver.builtIns, logger, "property: $declaredFq")

                val kind = if (isQtListModel) PropertyKind.QT_LIST_MODEL else PropertyKind.QT_PROPERTY

                Property(
                    kind = kind,
                    name = name,
                    notifySignalSignature = notify,
                    constant = constant,
                    writableFromQml = writable,
                    type = mapped,
                    declaredTypeQualifiedName = declaredFq,
                    sourceLocation = sourceLocation
                )
            } else null
        }.toList()

    private fun buildSignals(klass: KSClassDeclaration): SignalField? {
        // @QMLSignals interface field; TODO add support for multiple fields
        val qmlSignalsField = klass.getDeclaredProperties()
            .firstOrNull { it.hasAnnotation(QMLSignals::class) }

        if (qmlSignalsField == null) return null

        val fieldName = qmlSignalsField.simpleName.asString()
        val ifaceDecl = qmlSignalsField.type.resolve().declaration as? KSClassDeclaration
        val ifaceQn = ifaceDecl?.qualifiedName?.asString()

        if (ifaceDecl?.classKind != ClassKind.INTERFACE || ifaceQn == null) {
            logger.error("@QMLSignals must be used on an interface-typed property.", qmlSignalsField)
            return null
        }

        val signals = ifaceDecl.getDeclaredFunctions().map { fn ->
            val paramJvmTypes = fn.parameters.map { p ->
                JvmType.fromKSType(p.type.resolve(), resolver.builtIns, logger, "signal-param: $p")
            }
            val javaSig = buildString {
                append(fn.simpleName.asString())
                append("(")
                append(paramJvmTypes.joinToString(",") { it.javaType })
                append(")")
            }
            val cppSig = buildString {
                append(fn.simpleName.asString())
                append("(")
                append(paramJvmTypes.joinToString(",") { it.cppType })
                append(")")
            }
            val cppParams: List<Pair<String, String>> = fn.parameters.map { parameter ->
                val jvm = JvmType.fromKSType(parameter.type.resolve(), resolver.builtIns, logger, "signal-param: $parameter")
                val name = parameter.name?.asString() ?: "arg" // fallback to 'arg'
                name to jvm.cppType
            }
            Signal(
                javaSignature = javaSig,
                cppSignature = cppSig,
                cppParams = cppParams,
                sourceLocation = sourceLocationOf(fn)
            )
        }.toList()

        return SignalField(
            fieldName = fieldName,
            interfaceQualifiedName = ifaceQn,
            signals = signals
        )
    }

    private fun findQmlCompleteHandlerName(klass: KSClassDeclaration): String? {
        val qmlCompleteQn = QMLComplete::class.qualifiedName ?: return null

        val annotated = klass.getDeclaredFunctions().filter { fn ->
            fn.annotations.any { ann ->
                ann.annotationType.resolve().declaration.qualifiedName?.asString() == qmlCompleteQn
            }
        }.toList()

        if (annotated.size > 1) {
            annotated.drop(1).forEach { extra ->
                logger.error("Only one @QMLComplete method is allowed per @QMLRegistrable class.", extra)
            }
        }

        val fn = annotated.firstOrNull() ?: return null

        if (fn.modifiers.contains(Modifier.JAVA_STATIC)) {
            logger.error("@QMLComplete method must be an instance (non-static) method.", fn)
            return null
        }
        if (fn.parameters.isNotEmpty()) {
            logger.error("@QMLComplete method must not declare parameters.", fn)
            return null
        }
        val ret = fn.returnType?.resolve()
        val returnsVoid = ret == null || JvmType.fromKSType(ret, resolver.builtIns, logger).javaType == "void"
        if (!returnsVoid) {
            logger.error("@QMLComplete method must return void (Unit).", fn)
            return null
        }

        return fn.simpleName.asString()
    }
}
