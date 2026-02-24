/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lang

import com.intellij.lang.Language

@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
internal object QmlLanguage : Language("QML") {
    override fun getDisplayName(): String = "QML"
}
