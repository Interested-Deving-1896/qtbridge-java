/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

internal object QmlFileType : LanguageFileType(QmlLanguage) {
    override fun getName(): String = "QML File"

    override fun getDescription(): String = ""

    override fun getDefaultExtension(): String = "qml"

    override fun getIcon(): Icon = QmlIcons.QML
}
