/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.documentation

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.Position
import org.qtproject.qt.qtbridge.ide.lsp.QmlLspClient

internal class QmlDocumentationTargetProvider : DocumentationTargetProvider {
    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        val element = file.findElementAt(offset) ?: return emptyList()
        return listOf(QmlDocumentationTarget(element, offset))
    }
}

private class QmlDocumentationTarget(private val element: PsiElement, private val offset: Int) : DocumentationTarget {

    override fun computePresentation() = TargetPresentation.builder("QML Documentation").presentation()

    override fun computeDocumentation(): DocumentationResult? {
        val project = element.project
        val file = element.containingFile.virtualFile ?: return null
        val doc = PsiDocumentManager.getInstance(project).getDocument(element.containingFile) ?: return null

        val line = doc.getLineNumber(offset)
        val col = offset - doc.getLineStartOffset(line)
        val lspPos = Position(line, col)

        val hover = QmlLspClient.fetchHover(project, file, lspPos) ?: return null
        return buildResult(hover)
    }

    private fun buildResult(hover: Hover): DocumentationResult {
        val rawMarkdown = when {
            hover.contents.isRight -> hover.contents.right.value

            hover.contents.isLeft -> {
                val left = hover.contents.left
                if (left.isEmpty()) "No content returned from LSP"
                else left.joinToString("\n\n") { markup ->
                    if (markup.isLeft) markup.left
                    else "```${markup.right.language}\n${markup.right.value}\n```"
                }
            }
            else -> "Unsupported LSP Hover format"
        }

        if (rawMarkdown.isBlank())
            return DocumentationResult.documentation("LSP returned empty content.")

        val html = StringBuilder().apply {
            append(DocumentationMarkup.DEFINITION_START)
            append("<b>QML Symbol</b>")
            append(DocumentationMarkup.DEFINITION_END)
            append(DocumentationMarkup.CONTENT_START)
            append(rawMarkdown.replace("\n", "<br/>"))
            append(DocumentationMarkup.CONTENT_END)
        }.toString()

        return DocumentationResult.documentation(html)
    }


    override fun createPointer(): Pointer<out DocumentationTarget> {
        val ptr = SmartPointerManager.createPointer(element)
        return Pointer {
            val el = ptr.dereference() ?: return@Pointer null
            QmlDocumentationTarget(el, offset)
        }
    }
}
