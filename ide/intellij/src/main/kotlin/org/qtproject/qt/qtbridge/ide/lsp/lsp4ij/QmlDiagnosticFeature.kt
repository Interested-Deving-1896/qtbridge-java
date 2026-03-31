/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp.lsp4ij

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.redhat.devtools.lsp4ij.client.features.LSPDiagnosticFeature
import org.eclipse.lsp4j.Diagnostic
import org.qtproject.qt.qtbridge.ide.utils.highlightSeverity
import org.qtproject.qt.qtbridge.ide.utils.toolTip

@Suppress("UnstableApiUsage")
internal class QmlDiagnosticFeature : LSPDiagnosticFeature() {

    override fun getTooltip(diagnostic: Diagnostic): String {
        return diagnostic.toolTip()
    }

    override fun getHighlightSeverity(diagnostic: Diagnostic): HighlightSeverity? {
        return diagnostic.highlightSeverity() ?: super.getHighlightSeverity(diagnostic)
    }

    override fun createAnnotation(
        diagnostic: Diagnostic,
        document: Document,
        fixes: List<IntentionAction>,
        holder: AnnotationHolder
    ) {
        if (diagnostic.message.isNullOrBlank()) return
        super.createAnnotation(diagnostic, document, fixes, holder)
    }
}
