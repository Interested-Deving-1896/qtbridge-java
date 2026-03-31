/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp.lsp4ij

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.client.features.LSPHoverFeature
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import org.eclipse.lsp4j.MarkupContent
import org.qtproject.qt.qtbridge.ide.lsp.LspRuntimeDetector
import org.qtproject.qt.qtbridge.ide.utils.hoverContent

@Suppress("UnstableApiUsage")
internal class QmlLspServerFactory : LanguageServerFactory {
    private val logger = logger<QmlLspServerFactory>()
    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return if (LspRuntimeDetector.isNativeLspSupported(project)) {
            logger.info("Native LSP detected. Initializing DisabledProvider to avoid conflict.")
            DisabledLspProvider
        } else {
            logger.info("No native LSP found. Initializing QmlLsp4IJServer.")
            QmlLspIJProvider(project)
        }
    }
    override fun createClientFeatures() = LSPClientFeatures().apply {
        diagnosticFeature = QmlDiagnosticFeature()
        completionFeature = QmlCompletionFeature()
        hoverFeature = object : LSPHoverFeature() {
            override fun getContent(content: MarkupContent, file: PsiFile) = content.hoverContent()
        }
    }
}
