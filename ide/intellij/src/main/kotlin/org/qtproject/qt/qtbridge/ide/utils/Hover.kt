/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.utils

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.UIUtil
import org.eclipse.lsp4j.MarkupContent

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
