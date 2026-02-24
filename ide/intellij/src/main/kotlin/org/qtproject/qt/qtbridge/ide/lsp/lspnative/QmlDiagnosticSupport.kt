/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp.lspnative

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.platform.lsp.api.customization.LspDiagnosticsSupport
import org.eclipse.lsp4j.Diagnostic
import org.qtproject.qt.qtbridge.ide.utils.highlightSeverity
import org.qtproject.qt.qtbridge.ide.utils.toolTip

internal class QmlDiagnosticSupport : LspDiagnosticsSupport() {

    override fun getTooltip(diagnostic: Diagnostic): String {
        return diagnostic.toolTip()
    }

    override fun getHighlightSeverity(diagnostic: Diagnostic): HighlightSeverity? {
        return diagnostic.highlightSeverity() ?: super.getHighlightSeverity(diagnostic)
    }

    override fun createAnnotation(
        holder: AnnotationHolder,
        diagnostic: Diagnostic,
        textRange: TextRange,
        quickFixes: List<IntentionAction>
    ) {
        if (diagnostic.message.isNullOrBlank()) return
        super.createAnnotation(holder, diagnostic, textRange, quickFixes)
    }
}
