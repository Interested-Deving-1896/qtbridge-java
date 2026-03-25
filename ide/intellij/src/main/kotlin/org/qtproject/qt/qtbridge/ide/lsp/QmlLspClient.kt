/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.redhat.devtools.lsp4ij.LanguageServerManager
import kotlinx.coroutines.withTimeoutOrNull
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.services.LanguageServer
import org.qtproject.qt.qtbridge.ide.lsp.lspnative.QmlLspNativeProvider
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.milliseconds

internal object QmlLspClient {
    private const val TIMEOUT_MS = 1000L
    private val logger = logger<QmlLspClient>()

    fun fetchHover(project: Project, file: VirtualFile, pos: Position): Hover? {
        val params = HoverParams(TextDocumentIdentifier(file.toNioPath().toUri().toString()), pos)
        return withServer(project) { hover(project, params) }
    }

    fun restart(project: Project) {
        val backend = if (LspRuntimeDetector.isNativeLspSupported(project)) NativeLspBackend else Lsp4ijBackend
        try {
            backend.restart(project)
        } catch (t: Throwable) {
            logger.warn("LSP backend restart failed", t)
        }
    }

    private fun <T> withServer(project: Project, block: LspBackend.() -> T): T? {
        val backend = if (LspRuntimeDetector.isNativeLspSupported(project)) NativeLspBackend else Lsp4ijBackend

        return try {
            backend.block()
        } catch (t: Throwable) {
            logger.warn("LSP backend call failed", t)
            null
        }
    }

    private interface LspBackend {
        fun hover(project: Project, params: HoverParams): Hover?
        fun restart(project: Project)
    }

    private object NativeLspBackend : LspBackend {

        override fun hover(project: Project, params: HoverParams): Hover? {
            return runBlockingCancellable {
                withTimeoutOrNull(TIMEOUT_MS.milliseconds) {
                    resolveServer(project)?.sendRequest { it.textDocumentService.hover(params) }
                }
            }
        }

        override fun restart(project: Project) {
            LspServerManager.getInstance(project).stopAndRestartIfNeeded(QmlLspNativeProvider::class.java)
        }

        private fun resolveServer(project: Project): LspServer? {
            return LspServerManager.getInstance(project)
                .getServersForProvider(QmlLspNativeProvider::class.java)
                .firstOrNull()
                .also { if (it == null) logger.warn("No native QML LSP server found for project: $project") }
        }
    }

    private object Lsp4ijBackend : LspBackend {
        private const val SERVER_ID = "qml-ls" // matches id in lsp4ij-support.xml

        override fun hover(project: Project, params: HoverParams): Hover? {
            return resolveServer(project)
                ?.textDocumentService
                ?.hover(params)
                ?.await()
        }

        override fun restart(project: Project) {
            val manager = LanguageServerManager.getInstance(project)
            with(manager) {
                stop(SERVER_ID, LanguageServerManager.StopOptions().apply { isWillDisable = false })
                start(SERVER_ID, LanguageServerManager.StartOptions().apply { isForceStart = true })
            }
        }

        private fun resolveServer(project: Project): LanguageServer? {
            return LanguageServerManager.getInstance(project)
                .getLanguageServer(SERVER_ID)
                .await()?.server
                .also { if (it == null) logger.warn("No Lsp4ij server found for SERVER_ID: $SERVER_ID") }
        }
    }

    private fun <T> CompletableFuture<T>.await(): T? {
        return runCatching { get(TIMEOUT_MS, TimeUnit.MILLISECONDS) }
            .onFailure { cause ->
                when (cause) {
                    is TimeoutException -> logger.debug("LSP request timed out after ${TIMEOUT_MS}ms")
                    else -> logger.warn("LSP future failed", cause)
                }
            }.getOrNull()
    }
}
