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
import org.qtproject.qt.bridge.annotations.QMLSignal
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

    // Builds VariableInfo from a JvmType
    private fun JvmType.toVariableInfo(isEnumType: Boolean = false): VariableInfo {
        val shape = when {
            javaType.endsWith("[]") -> VariableShape.ARRAY
            javaType == "java.util.List" -> VariableShape.LIST
            javaType == "java.util.Map" -> VariableShape.MAP
            else -> VariableShape.VALUE
        }

        // Determine element/value type from mapped Java type / C++ type
        val type = if (isEnumType) {
            VariableType.ENUM
        } else when (javaType) {
            "boolean", "java.lang.Boolean", "boolean[]", "java.lang.Boolean[]" -> VariableType.BOOLEAN
            "byte", "java.lang.Byte", "byte[]", "java.lang.Byte[]" -> VariableType.BYTE
            "char", "java.lang.Character", "char[]", "java.lang.Character[]" -> VariableType.CHAR
            "short", "java.lang.Short", "short[]", "java.lang.Short[]" -> VariableType.SHORT
            "int", "java.lang.Integer", "int[]", "java.lang.Integer[]" -> VariableType.INT
            "long", "java.lang.Long", "long[]", "java.lang.Long[]" -> VariableType.LONG
            "float", "java.lang.Float", "float[]", "java.lang.Float[]" -> VariableType.FLOAT
            "double", "java.lang.Double", "double[]", "java.lang.Double[]" -> VariableType.DOUBLE
            "java.lang.String", "java.lang.String[]" -> VariableType.STRING
            "void" -> VariableType.VOID
            else -> if (cppType == "QObject*") VariableType.QML_REGISTRABLE
                    else if (cppType == "QAbstractItemModel*") VariableType.ITEM_MODEL
                    else VariableType.STRING // Not sure of the default, can this happen?
        }

        return VariableInfo(shape, type, isPrimitive)
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
    fun generate(klass: KSClassDeclaration): RegistrableClass? {
        // Bail out if the whole class is ignored
        if (klass.isQmlIgnored())
            return null

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
        val signalField = buildSignalsFromSignalField(classHierarchy)
        val signalMethods = buildSignalsFromSignalMethods(classHierarchy)
        // A _QtImpl subclass is generated to override @QMLSignal methods, so the user's
        // class must itself be extendable (abstract or open in Kotlin; non-final in Java).
        if (signalMethods.isNotEmpty() && Modifier.FINAL in klass.modifiers) {
            logger.error(
                "Class '${klass.simpleName.asString()}' has @QMLSignal methods but is final. " +
                "Declare it as 'open' or 'abstract' so the bridge can generate a subclass." +
                "Alternatively consider using @QMLSignals field.", klass
            )
            return null
        }
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
            signalMethods = signalMethods,
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
        // Keep track of QMLIgnored invokables so that we deal with overloads properly
        val ignored = HashSet<String>()

        classHierarchy.forEach { klass ->
            klass.getDeclaredFunctions()
                // Only accept public non-constructor functions, that are not also annotated as QMLSignal
                .filter { it.isPublic() && !it.isConstructor() && !it.hasAnnotation(QMLSignal::class) }
                .forEach { invokable ->
                    val returnType = invokable.returnType?.resolve()
                    val returnJvm = JvmType.fromKSType(returnType, resolver.builtIns, logger, "return: $invokable")
                    val paramTypes = invokable.parameters.map { p -> p.type.resolve() }
                    val paramJvmTypes = invokable.parameters.mapIndexed { index, p ->
                        JvmType.fromKSType(paramTypes[index], resolver.builtIns, logger, "parameter: $p")
                    }

                    val javaSignature = buildString {
                        append(invokable.simpleName.asString())
                        append("(")
                        append(paramJvmTypes.joinToString(",") { it.javaType })
                        append(")")
                    }

                    // If invokable is QMLIgnored, add it on the ignored list and remove
                    // any potential invokable a parent class might have set previously
                    if (invokable.isQmlIgnored()) {
                        ignored += javaSignature
                        invokables.remove(javaSignature)
                        return@forEach
                    }
                    // If invokable was QMLIgnored previously by a base class, keep ignoring
                    // it (derived classes cannot re-enable it).
                    if (ignored.contains(javaSignature))
                        return@forEach

                    val cppSignature = buildString {
                        append(invokable.simpleName.asString())
                        append("(")
                        append(paramJvmTypes.joinToString(",") { it.cppType })
                        append(")")
                    }

                    // List of 'name, type' arguments for the function
                    val cppParams: List<Pair<String, String>> = invokable.parameters.map { parameter ->
                        val jvm = JvmType.fromKSType(parameter.type.resolve(), resolver.builtIns, logger, "parameter: $parameter")
                        val name = parameter.name?.asString() ?: "arg"  // fallback to generic 'arg'
                        name to jvm.cppType
                    }

                    val invokable = Invokable(
                        name = invokable.simpleName.asString(),
                        javaSignature = javaSignature,
                        cppSignature = cppSignature,
                        cppParams = cppParams,
                        javaReturnType = returnJvm.javaType,
                        cppReturnType = returnJvm.cppType,
                        retInfo = returnJvm.toVariableInfo(returnType?.isEnum() == true),
                        paramListInfo = paramJvmTypes.mapIndexed { index, jvm ->
                            jvm.toVariableInfo(paramTypes[index].isEnum())
                        },
                        sourceLocation = sourceLocationOf(invokable)
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
        // Keep track of QMLIgnored invokables so that we deal with overloads properly
        val ignored = HashSet<String>()

        classHierarchy.forEach { klass ->
            klass.getDeclaredProperties().forEach { prop ->
                val name = prop.simpleName.asString()

                // If property is QMLIgnored, add it on the ignored list and remove
                // any potential property a parent class might have set previously
                if (prop.isQmlIgnored()) {
                    ignored += name
                    properties.remove(name)
                    return@forEach
                }
                // If property was QMLIgnored previously by a base class, keep ignoring
                // it (derived classes cannot re-enable it).
                if (ignored.contains(name))
                    return@forEach

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
                        typeInfo = mapped.toVariableInfo(valueType.isEnum()),
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

    // @QMLSignals field
    private fun buildSignalsFromSignalField(classHierarchy: List<KSClassDeclaration>): SignalField? {
        // Use the signal field of the most derived child
        val qmlSignalsFields = classHierarchy
            .asReversed() // child -> base
            .flatMap { it.getDeclaredProperties().filter { p -> p.hasAnnotation(QMLSignals::class) } }

        val field = qmlSignalsFields.firstOrNull() ?: return null
        // If the current (leaf) class's QMLSignals field itself is QMLIgnored, ignore all signals
        if (field.isQmlIgnored())
            return null

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
        // Keep track of QMLIgnored invokables so that we deal with overloads properly
        val ignored = HashSet<String>()

        signalHierarchy.forEach { iDecl ->
            iDecl.getDeclaredFunctions().forEach { fn ->
                // Note: with signal field the javaSignature must match the signature generated
                // by QtSignalProxy as it is used as the cache lookup key
                val signal = buildSignalFromFunction(fn)

                // If signal is QMLIgnored, add it on the ignored list and remove
                // any potential signal a parent class might have set previously
                if (fn.isQmlIgnored()) {
                    ignored += signal.javaSignature
                    signals.remove(signal.javaSignature)
                    return@forEach
                }
                // If signal was QMLIgnored previously by a base class, keep ignoring
                // it (derived classes cannot re-enable it).
                if (ignored.contains(signal.javaSignature))
                    return@forEach

                // Ensure override from more-derived interface wins.
                signals.remove(signal.javaSignature)
                signals[signal.javaSignature] = signal
            }
        }
        return SignalField(
            fieldName = fieldName,
            interfaceQualifiedName = ifaceQn,
            signals = signals.values.toList()
        )
    }

    // Individual @QMLSignal methods
    private fun buildSignalsFromSignalMethods(classHierarchy: List<KSClassDeclaration>): List<Signal> {
        val signals = LinkedHashMap<String, Signal>()
        val ignored = HashSet<String>()

        // Traverse from base -> child
        classHierarchy.forEach { klass ->
            klass.getDeclaredFunctions()
            .filter { it.hasAnnotation(QMLSignal::class) }
            .forEach { method ->
                // Signal must return void/Unit
                val returnQn = method.returnType?.resolve()?.declaration?.qualifiedName?.asString()
                if (returnQn != "kotlin.Unit") {
                    logger.error("@QMLSignal method '${method.simpleName.asString()}' must return void/Unit", method)
                    return@forEach
                }
                // Signal must be abstract or open (not final) so that we can override it
                if (Modifier.FINAL in method.modifiers) {
                    logger.error("@QMLSignal method '${method.simpleName.asString()}' must be abstract or open", method)
                    return@forEach
                }

                // Note: The javaSignature is used as the JNICache lookup key when signal is emitted
                val signal = buildSignalFromFunction(method)

                // Add ignored signal on the ignored list and remove
                // any potential signal a parent class might have set previously
                if (method.isQmlIgnored()) {
                    ignored += signal.javaSignature
                    signals.remove(signal.javaSignature)
                    return@forEach
                }
                // If signal is ignored by base class, don't allow child class to re-enable it
                if (ignored.contains(signal.javaSignature))
                    return@forEach

                signals.remove(signal.javaSignature)
                signals[signal.javaSignature] = signal
            }
        }
        return signals.values.toList()
    }

    // Builds a Signal from KSFunctionDeclaration
    private fun buildSignalFromFunction(fn: KSFunctionDeclaration): Signal {
        val paramTypes = fn.parameters.map { it.type.resolve() }
        val paramJvmTypes = fn.parameters.mapIndexed { index, p ->
            JvmType.fromKSType(paramTypes[index], resolver.builtIns, logger, "signal-param: $p")
        }
        val name = fn.simpleName.asString()
        val javaSignature = "$name(${paramJvmTypes.joinToString(",") { it.javaType }})"
        val cppSignature  = "$name(${paramJvmTypes.joinToString(",") { it.cppType }})"
        val cppParams = fn.parameters.mapIndexed { index, parameter ->
            (parameter.name?.asString() ?: "arg$index") to paramJvmTypes[index].cppType
        }
        val methodOverrideParams =  fn.parameters.mapIndexed { index, parameter ->
                (parameter.name?.asString() ?: "arg$index") to ksTypeToKotlinTypeName(paramTypes[index])
        }

        return Signal(
            javaSignature = javaSignature,
            cppSignature = cppSignature,
            cppParams = cppParams,
            paramListInfo = paramJvmTypes.mapIndexed { index, jvm ->
                jvm.toVariableInfo(paramTypes[index].isEnum())
            },
            sourceLocation = sourceLocationOf(fn),
            methodOverrideParams = methodOverrideParams,
        )
    }

    // Converts KSType to a Kotlin type name, which we can then use to build overriding
    // signatures (_QtImpl). In theory this could be crammed into JvmType which already maps
    // KSTypes to Java and Cpp types. But that would complicate it unnecessarily, as it's
    // straightforward to derive Kotlin type from KSType
    private fun ksTypeToKotlinTypeName(ksType: KSType): String {
        val typeName = ksType.declaration.qualifiedName!!.asString()
        val typeArgs = ksType.arguments
        val result = if (typeArgs.isEmpty()) {
            // Type has no additional parameters
            typeName
        } else {
            // Type has additional parameters (eg. "List<String, List<Int>>") -> recurse
            val argStr = typeArgs.joinToString(", ") { arg ->
                ksTypeToKotlinTypeName(arg.type!!.resolve())
            }
            "$typeName<$argStr>"
        }
        return result
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
