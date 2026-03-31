/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default

internal object QmlColors {
    private fun key(id: String, fallback: TextAttributesKey) = createTextAttributesKey(id, fallback)

    internal object Syntax {
        val IDENTIFIER = key("QML_IDENTIFIER", Default.IDENTIFIER)
        val QML_ID = key("QML_ID", Default.IDENTIFIER)
        val QML_KEYWORD = key("QML_KEYWORD", Default.KEYWORD)
        val TYPE_NAME = key("QML_TYPE_NAME", Default.CLASS_NAME)
        val SIGNAL_HANDLER = key("QML_SIGNAL_HANDLER", Default.INSTANCE_METHOD)
        val ENUM_VALUE = key("QML_ENUM_VALUE", Default.STRING)
        val STRING = key("QML_STRING", Default.STRING)
        val NUMBER = key("QML_NUMBER", Default.NUMBER)
        val OPERATOR = key("QML_OPERATOR", Default.OPERATION_SIGN)
    }
}
