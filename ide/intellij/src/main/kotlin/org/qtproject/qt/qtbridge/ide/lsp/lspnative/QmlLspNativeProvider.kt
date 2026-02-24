/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp.lspnative

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import org.qtproject.qt.qtbridge.ide.lsp.LspRuntimeDetector

internal class QmlLspNativeProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        val lspDescriptor = QmlLspNativeDescriptor(project)
        if (lspDescriptor.isSupportedFile(file) && LspRuntimeDetector.isNativeLspSupported(project)) {
            serverStarter.ensureServerStarted(lspDescriptor)
        }
    }
}
