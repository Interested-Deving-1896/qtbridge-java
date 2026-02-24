/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.utils

import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.InsertTextFormat
import org.eclipse.lsp4j.TextEdit

internal val CompletionItem.isHidden: Boolean
    get() = label in setOf(
        "__defineGetter__", "__defineSetter__", "__lookupGetter__", "__lookupSetter__", "__proto__", "constructor",
        "hasOwnProperty", "isPrototypeOf", "propertyIsEnumerable", "toLocaleString", "destroy", "valueOf"
    )

internal val CompletionItem.isCallable: Boolean
    get() = kind in setOf(CompletionItemKind.Method, CompletionItemKind.Function)

private const val POSITION = $$"$0"

internal fun CompletionItem.completeWithParentheses() {
    val newInsertText = "$label($POSITION)"

    insertTextFormat = InsertTextFormat.Snippet

    when (val textEdit = textEdit?.get()) {
        null -> insertText = newInsertText
        is TextEdit -> textEdit.newText = newInsertText
    }
}
