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
        val includeSuper = ann.arg<Boolean>("includeSuper") ?: true

        val typeName = name.ifBlank { simpleName.asString() }
        val moduleName = module.ifBlank { "QtBridge" }
        return RegistrableInfo(typeName, moduleName, singleton, includeSuper)
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
        val classHierarchy =
            if (registrableInfo?.includeSuper == false)
                listOf(klass)
            else
                collectClassHierarchy(klass)
        val invokables = buildInvokables(classHierarchy)
        val properties = buildProperties(classHierarchy)
        val signalField = buildSignals(classHierarchy)
        // Determine first QMLRegistrable parent, if any. This inheritance
        // information is needed when generating MOC json tooling data
        val registrableSuperClass: String? =
            if (registrableInfo?.includeSuper == false)
                null
            else classHierarchy.dropLast(1) // don't consider self/leaf
                               .asReversed() // start from leaf / bottom
                               .firstOrNull { it.QMLRegistrableInfo() != null} // first class that is QMLRegistrable (or null)
                               ?.simpleName
                               ?.asString()

        return RegistrableClass(
            cppType = "QObject", // Fixed atm, may change if we support visual types (QQuickItem)
            packageName = pkg,
            simpleName = simple,
            qualifiedName = qualified,
            registrableSuperClass = registrableSuperClass,
            registrableInfo = registrableInfo,
            sourceLocation = sourceLocationOf(klass),
            invokables = invokables,
            properties = properties,
            signalField = signalField,
            qmlCompleteHandlerName = findQmlCompleteHandlerName(klass),
        )
    }

    // Returns list of classes from least-derived (parent) to most-derived (child) class
    private fun collectClassHierarchy(klass: KSClassDeclaration): List<KSClassDeclaration> {
        val classes = mutableListOf<KSClassDeclaration>()
        var current: KSClassDeclaration? = klass

        while (current != null) {
            classes += current

            // Find the immediate superclass (ignore interfaces)
            val superClass = current.superTypes
                .mapNotNull { it.resolve().declaration as? KSClassDeclaration }
                .firstOrNull { it.classKind == ClassKind.CLASS }

            if (superClass == null)
                break

            val qualifiedName = superClass.qualifiedName?.asString()
            // Ignore top-most Object / Any
            if (qualifiedName == "kotlin.Any" || qualifiedName == "java.lang.Object")
                break

            current = superClass
        }
        return classes.asReversed()
    }

    // Returns list of interfaces from least-derived (parent) to most-derived (child) class
    private fun collectSignalInterfaceHierarchy(iface: KSClassDeclaration): List<KSClassDeclaration> {
        // Collect from child to base, then reverse to get base -> child order.
        // Return in base -> derived order so that most-derived wins when merging.
        require(iface.classKind == ClassKind.INTERFACE)

        val interfaces = LinkedHashMap<String, KSClassDeclaration>()

        fun visit(iface: KSClassDeclaration) {
            val qualifiedName = iface.qualifiedName?.asString() ?: return
            // Ignore top-most Object / Any
            if (qualifiedName == "kotlin.Any" || qualifiedName == "java.lang.Object")
                return
            if (interfaces.containsKey(qualifiedName))
                return

            // Visit parents first so ordering becomes base -> derived.
            iface.superTypes
                .mapNotNull { it.resolve().declaration as? KSClassDeclaration }
                .filter { it.classKind == ClassKind.INTERFACE }
                .forEach { visit(it) }

            interfaces[qualifiedName] = iface
        }

        visit(iface)
        return interfaces.values.toList()
    }

    private fun buildInvokables(classHierarchy: List<KSClassDeclaration>): List<Invokable> {
        // Use Java-signature as key and for conflict resolution (most-derived class wins)
        val invokables = LinkedHashMap<String, Invokable>()

        classHierarchy.forEach { klass ->
            klass.getDeclaredFunctions()
                .filter { it.isPublic() && !it.isConstructor() }
                .forEach { fn ->
                    val returnJvm = JvmType.fromKSType(fn.returnType?.resolve(), resolver.builtIns, logger, "return: $fn")
                    val paramJvmTypes = fn.parameters.map { p ->
                        JvmType.fromKSType(p.type.resolve(), resolver.builtIns, logger, "parameter: $p")
                    }

                    val javaSignature = buildString {
                        append(fn.simpleName.asString())
                        append("(")
                        append(paramJvmTypes.joinToString(",") { it.javaType })
                        append(")")
                    }
                    val cppSignature = buildString {
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

                    val invokable = Invokable(
                        name = fn.simpleName.asString(),
                        javaSignature = javaSignature,
                        cppSignature = cppSignature,
                        cppParams = cppParams,
                        javaReturnType = returnJvm.javaType,
                        cppReturnType = returnJvm.cppType,
                        retIsPrimitive = returnJvm.isPrimitive,
                        paramIsPrimitive = paramJvmTypes.map { it.isPrimitive }.toBooleanArray(),
                        sourceLocation = sourceLocationOf(fn)
                    )

                    // Ensure that in case of override / conflict that most-derived entry wins
                    invokables.remove(javaSignature)
                    invokables[javaSignature] = invokable
                }
        }
        return invokables.values.toList()
    }

    private fun buildProperties(classHierarchy: List<KSClassDeclaration>): List<Property> {
        // Use properties' name as key and for conflict resolution (most-derived class wins)
        val properties = LinkedHashMap<String, Property>()

        classHierarchy.forEach { klass ->
            klass.getDeclaredProperties().forEach { prop ->
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
                        return@forEach
                    }

                    var constant = false
                    var writable = true
                    if (isQtListModel) {
                        constant = true
                        writable = false
                    } else if (valueType.isEnum()) {
                        writable = false // Enums are not writable (QTBUG-141710)
                    }

                    val declaredFq = (propType.declaration as? KSClassDeclaration)?.qualifiedName?.asString()
                    val mapped = JvmType.fromKSType(valueType, resolver.builtIns, logger, "property: $declaredFq")

                    val kind = if (isQtListModel) PropertyKind.QT_LIST_MODEL else PropertyKind.QT_PROPERTY

                    val property = Property(
                        kind = kind,
                        name = name,
                        notifySignalSignature = notify,
                        constant = constant,
                        writableFromQml = writable,
                        type = mapped,
                        declaredTypeQualifiedName = declaredFq,
                        sourceLocation = sourceLocation
                    )
                    // Ensure that in case of override / conflict that most-derived entry wins
                    properties.remove(name)
                    properties[name] = property
                }
            }
        }
        return properties.values.toList()
    }

    private fun buildSignals(classHierarchy: List<KSClassDeclaration>): SignalField? {
        // Use the signal field of the most derived child
        val qmlSignalsFields = classHierarchy
            .asReversed() // child -> base
            .flatMap { it.getDeclaredProperties().filter { p -> p.hasAnnotation(QMLSignals::class) } }

        if (qmlSignalsFields.size > 1) {
            qmlSignalsFields.drop(1).forEach { extra ->
                logger.warn(
                    "Multiple @QMLSignals fields found in class hierarchy. " +
                    "Using the one in the most derived class.",
                    extra)
            }
        }

        val field = qmlSignalsFields.firstOrNull() ?: return null

        val fieldName = field.simpleName.asString()
        val ifaceDecl = field.type.resolve().declaration as? KSClassDeclaration
        val ifaceQn = ifaceDecl?.qualifiedName?.asString()

        if (ifaceDecl?.classKind != ClassKind.INTERFACE || ifaceQn == null) {
            logger.error("@QMLSignals must be used on an interface-typed property.", field)
            return null
        }

        // Get all signals of the interface, including all inherited signals
        val signalHierarchy = collectSignalInterfaceHierarchy(ifaceDecl)
        // Use Java signature as key and for conflict resolution (most-derived class wins)
        val signals = LinkedHashMap<String, Signal>()

        signalHierarchy.forEach { iDecl ->
            iDecl.getDeclaredFunctions().forEach { fn ->
                val paramJvmTypes = fn.parameters.map { p ->
                    JvmType.fromKSType(p.type.resolve(), resolver.builtIns, logger, "signal-param: $p")
                }
                val javaSignature = buildString {
                    append(fn.simpleName.asString())
                    append("(")
                    append(paramJvmTypes.joinToString(",") { it.javaType })
                    append(")")
                }
                val cppSignature = buildString {
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
                val signal = Signal(
                    javaSignature = javaSignature,
                    cppSignature = cppSignature,
                    cppParams = cppParams,
                    sourceLocation = sourceLocationOf(fn)
                )

                // Ensure override from more-derived interface wins.
                signals.remove(javaSignature)
                signals[javaSignature] = signal
            }
        }
        return SignalField(
            fieldName = fieldName,
            interfaceQualifiedName = ifaceQn,
            signals = signals.values.toList()
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
