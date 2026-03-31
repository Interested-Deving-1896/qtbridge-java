/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.utils

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.UIUtil
import org.eclipse.lsp4j.MarkupContent

internal fun Project.resolveQmlLsCommandLine(): GeneralCommandLine? {
    return QmlToolingInfoParser.parse(this)?.let { QmlToolCommand.QmlLs.buildCommandLine(it.toQmlLSConfig()) }
}

private val QUOTED_REGEX = Regex(""""([^"]*)"""")

internal fun HtmlChunk.Element.withFont(font: String?): HtmlChunk.Element {
    return if (font != null) this.style("font-family: '$font', monospace; font-size: 0.9em;") else this
}

internal fun String.formatAsHtml(): String {
    val escaped = StringUtil.escapeXmlEntities(this)
    return escaped.replace(QUOTED_REGEX) { "<b>${it.value}</b>" }
}

internal fun MarkupContent.hoverContent(): String {
    val scheme = EditorColorsManager.getInstance().globalScheme
    val editorFont = scheme.editorFontName

    return HtmlBuilder()
        .append(
            HtmlChunk.div()
                .style("padding: 4px 8px;")
                .withFont(editorFont)
                .children(
                    HtmlChunk.span()
                        .style("color: ${ColorUtil.toHtmlColor(UIUtil.getLabelForeground())};")
                        .addRaw(formattedValue())
                )
        ).toString()
}

private fun MarkupContent.formattedValue(): String {
    return (value ?: "").trim().formatAsHtml()
}
