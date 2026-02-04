/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import org.qtproject.qt.bridge.generator.*
import org.qtproject.qt.bridge.annotations.QMLRegistrable
import org.qtproject.qt.bridge.annotations.QMLSignals
import org.qtproject.qt.bridge.utils.containingClass
import kotlin.collections.flatMap

internal class QtBridgeProcessor(
    codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private lateinit var classModelGenerator: ClassModelGenerator
    private val classCreationEmitter = ClassCreationEmitter(codeGenerator)
    private val classMocJsonEmitter = ClassMocJsonEmitter(codeGenerator)
    private val qmlRegistrables = mutableListOf<RegistrableClass>()

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val allAnnotations = listOf(
            QMLSignals::class,
            QMLRegistrable::class
        ).mapNotNull { it.qualifiedName }

        val annotatedSymbols = allAnnotations.flatMap { annotation ->
            resolver.getSymbolsWithAnnotation(annotation)
        }

        val annotatedClasses = annotatedSymbols
            .mapNotNull { it.containingClass() }
            .distinct()

        classModelGenerator = ClassModelGenerator(resolver, logger)

        for (clazz in annotatedClasses) {
            val model = classModelGenerator.generate(clazz) ?: continue
            qmlRegistrables += model
            classCreationEmitter.emitClassFromModel(model)
        }
        return emptyList()
    }

    override fun finish() {
        // Emit code that can be called to bootstrap the QML type registration
        classCreationEmitter.emitQmlTypeRegistrationEntryPoint(qmlRegistrables)
        // Emit code that is used by qmltyperegistrar to generate .qmltypes file
        // for each QML module
        classMocJsonEmitter.emitMocJsonPerModule(qmlRegistrables)
    }
}
