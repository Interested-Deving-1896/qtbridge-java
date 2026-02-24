/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp.lsp4ij

import com.intellij.codeInsight.lookup.LookupElement
import com.redhat.devtools.lsp4ij.client.features.LSPCompletionFeature
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.InsertTextFormat
import org.eclipse.lsp4j.TextEdit
import org.qtproject.qt.qtbridge.ide.utils.completeWithParentheses
import org.qtproject.qt.qtbridge.ide.utils.isCallable
import org.qtproject.qt.qtbridge.ide.utils.isHidden

internal class QmlCompletionFeature : LSPCompletionFeature() {
    override fun createLookupElement(item: CompletionItem, context: LSPCompletionContext): LookupElement? {
        if (item.isHidden)
            return null
        if (item.isCallable)
            item.completeWithParentheses()
        return super.createLookupElement(item, context)
    }
}
