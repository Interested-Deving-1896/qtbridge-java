/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp.lspnative

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.platform.lsp.api.customization.LspCompletionSupport
import org.eclipse.lsp4j.CompletionItem
import org.qtproject.qt.qtbridge.ide.utils.completeWithParentheses
import org.qtproject.qt.qtbridge.ide.utils.isCallable
import org.qtproject.qt.qtbridge.ide.utils.isHidden

internal class QmlCompletionSupport : LspCompletionSupport() {

    override fun createLookupElement(parameters: CompletionParameters, item: CompletionItem): LookupElement? {
        if (item.isHidden)
            return null
        if (item.isCallable)
            item.completeWithParentheses()
        return super.createLookupElement(parameters, item)
    }
}
