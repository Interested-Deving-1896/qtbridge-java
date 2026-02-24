/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp

import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerManager
import com.redhat.devtools.lsp4ij.LanguageServerManager
import kotlinx.coroutines.withTimeoutOrNull
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.qtproject.qt.qtbridge.ide.lsp.lspnative.QmlLspNativeProvider
import java.util.concurrent.TimeUnit

internal object QmlLspClient {

    private const val TIMEOUT_MS = 1000L

    fun fetchHover(project: Project, file: VirtualFile, pos: Position): Hover? {
        val params = HoverParams(TextDocumentIdentifier(file.toNioPath().toUri().toString()), pos)
        return if (LspRuntimeDetector.isNativeLspSupported(project)) {
            tryNativeLsp(project, file, params)
        } else {
            tryLsp4ij(project, params)
        }
    }

    private fun tryNativeLsp(project: Project, file: VirtualFile, params: HoverParams): Hover? {
        val server = LspServerManager.getInstance(project)
            .getServersForProvider(QmlLspNativeProvider::class.java)
            .firstOrNull { it.descriptor.isSupportedFile(file) }
            ?: return null

        return runBlockingCancellable {
            withTimeoutOrNull(TIMEOUT_MS) {
                runCatching { server.sendRequest { it.textDocumentService.hover(params) } }
                    .getOrNull()
            }
        }
    }

    private fun tryLsp4ij(project: Project, params: HoverParams): Hover? {
        val serverItem = runCatching {
            LanguageServerManager.getInstance(project)
                .getLanguageServer("qml-ls")
                .get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }.getOrNull() ?: return null

        return runCatching {
            serverItem.server
                ?.textDocumentService
                ?.hover(params)
                ?.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }.getOrNull()
    }
}
