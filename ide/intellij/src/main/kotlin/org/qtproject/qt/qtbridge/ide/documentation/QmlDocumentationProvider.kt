/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.documentation

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement

internal class QmlDocumentationProvider : DocumentationProvider {

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String {
        return buildHoverHtml(element)
    }

    private fun buildHoverHtml(element: PsiElement?): String {
        element ?: return "<html><body><i>No element found</i></body></html>"

        val definition = element.text.trim().trimEnd { it == '{' || it == ';' || it == ' ' }

        val kind = when {
            definition.contains("class") -> "Class"
            definition.contains("QtProperty") -> "QtProperty"
            definition.contains(")") -> "Method"
            else -> "Field"
        }

        val file = element.containingFile?.name ?: "Unknown"

        return """
            <html><body style="font-family: sans-serif; padding: 8px;">
                <div><b>$definition</b></div>
                <hr/>
                <div>Kind: $kind</div>
                <div>File: $file</div>
            </body></html>
        """.trimIndent()
    }

}
