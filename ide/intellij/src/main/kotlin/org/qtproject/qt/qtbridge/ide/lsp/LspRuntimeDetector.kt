/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp

import com.intellij.openapi.project.Project

internal object LspRuntimeDetector {
    fun isNativeLspSupported(project: Project): Boolean {
        return project.extensionArea.hasExtensionPoint("com.intellij.platform.lsp.serverSupportProvider")
    }
}
