/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.utils

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.util.text.StringUtil


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
