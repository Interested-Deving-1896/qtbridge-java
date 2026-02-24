/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp.lspnative

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspCustomization
import org.qtproject.qt.qtbridge.ide.lang.QmlFileType
import org.qtproject.qt.qtbridge.ide.lang.QmlLanguage
import org.qtproject.qt.qtbridge.ide.utils.resolveQmlLsCommandLine

internal class QmlLspNativeDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, QmlLanguage.displayName) {

    private val commandLine by lazy { project.resolveQmlLsCommandLine() }

    override val lspCustomization = object : LspCustomization() {
        override val diagnosticsCustomizer = QmlDiagnosticSupport()
        override val completionCustomizer = QmlCompletionSupport()
    }

    override fun isSupportedFile(file: VirtualFile): Boolean =
        file.extension == QmlFileType.defaultExtension && commandLine != null

    override fun createCommandLine(): GeneralCommandLine = commandLine!!
}
