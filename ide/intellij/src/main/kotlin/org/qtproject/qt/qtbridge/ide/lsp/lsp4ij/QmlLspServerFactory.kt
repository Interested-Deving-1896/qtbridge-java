/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp.lsp4ij

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.client.features.LSPHoverFeature
import com.redhat.devtools.lsp4ij.client.features.LSPSemanticTokensFeature
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import org.eclipse.lsp4j.MarkupContent
import org.qtproject.qt.qtbridge.ide.highlighting.QmlColors
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
        semanticTokensFeature = object : LSPSemanticTokensFeature() {
            override fun getTextAttributesKey(
                tokenType: String,
                tokenModifiers: MutableList<String>,
                file: PsiFile
            ): TextAttributesKey? {
                return when (tokenType) {
                    "type" -> QmlColors.Syntax.TYPE_NAME
                    "property" -> QmlColors.Syntax.IDENTIFIER
                    "variable", "parameter" -> QmlColors.Syntax.QML_ID
                    "method" -> QmlColors.Syntax.SIGNAL_HANDLER
                    "keyword" -> QmlColors.Syntax.QML_KEYWORD
                    "number" -> QmlColors.Syntax.NUMBER
                    "string" -> QmlColors.Syntax.STRING
                    "operator" -> QmlColors.Syntax.OPERATOR
                    "enumMember" -> QmlColors.Syntax.ENUM_VALUE
                    else -> super.getTextAttributesKey(tokenType, tokenModifiers, file)
                }
            }
        }
    }
}
