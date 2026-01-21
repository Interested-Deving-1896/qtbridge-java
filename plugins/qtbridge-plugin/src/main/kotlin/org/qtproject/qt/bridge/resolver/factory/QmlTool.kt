/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.resolver.factory

import org.qtproject.qt.bridge.utility.Platform

internal enum class QmlTool(tool: String, val subDirectory: String = "bin") {
    QMLLS(tool = "qmlls"),
    QMLLINT(tool = "qmllint"),
    QMLTYPEREGISTRAR(tool = "qmltyperegistrar", subDirectory = if (Platform.isWindows()) "bin" else "libexec");

    val toolName = if (Platform.isWindows()) "$tool.exe" else tool
}
