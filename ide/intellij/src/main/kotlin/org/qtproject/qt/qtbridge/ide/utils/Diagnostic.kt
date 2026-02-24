/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.utils

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity

private val CATEGORY_REGEX = Regex("""\[([^]]+)]$""")
private val QML_ERROR_IDS = setOf("missing-property", "unqualified")

fun Diagnostic.highlightSeverity(): HighlightSeverity? {
    val msg = message?.lowercase() ?: ""

    return when {
        QML_ERROR_IDS.any { msg.contains(it) } -> HighlightSeverity.ERROR
        severity == DiagnosticSeverity.Hint -> HighlightSeverity.WEAK_WARNING
        else -> null
    }
}

fun Diagnostic.toolTip(): String {
    val scheme = EditorColorsManager.getInstance().globalScheme
    val editorFont = scheme.editorFontName

    return HtmlBuilder()
        .append(HtmlChunk.div()
            .withFont(editorFont)
            .children(
                HtmlChunk.span().addRaw(formattedMessage()),
                HtmlChunk.hr().style("margin: 4px 0;"),
                buildMetadataSection(editorFont)
            ))
        .toString()
}

private fun Diagnostic.formattedMessage(): String {
    return (message ?: "").replace(CATEGORY_REGEX, "").trim().formatAsHtml()
}

private fun Diagnostic.buildMetadataSection(editorFont: String?): HtmlChunk {
    val labelColor = ColorUtil.toHtmlColor(UIUtil.getInactiveTextColor())

    val badgeBg = if (!JBColor.isBright()) "#45494A" else "#E8E8E8"
    val badgeStyle = "background-color: $badgeBg; padding: 1px 4px; border-radius: 2px;"

    return HtmlChunk.font(labelColor).children(
        HtmlChunk.text("Category: "),
        HtmlChunk.span().style(badgeStyle).child(category().toFormattedCategoryLink(editorFont)),
        HtmlChunk.nbsp(6),
        HtmlChunk.text("Source: "), HtmlChunk.span().style(badgeStyle).addText(source ?: "qmllint")
    )
}

private fun String.toFormattedCategoryLink(font: String?): HtmlChunk {
    val docUrl = "https://doc.qt.io/qt-6/qmllint-warnings-and-errors${if (this == "general") "" else "-$this"}.html"
    return HtmlChunk.link(docUrl, this)
        .withFont(font)
}

private fun Diagnostic.category(): String = CATEGORY_REGEX.find(message ?: "")?.groupValues?.get(1) ?: "general"
