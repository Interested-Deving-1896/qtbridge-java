/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.utils

import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import org.qtproject.qt.qtbridge.ide.lsp.QmlLspClient
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class QmlToolingFilesGenerationOnSave : FileDocumentManagerListener {
    private val logger = logger<QmlToolingFilesGenerationOnSave>()
    private val pendingTask = AtomicReference<ProgressIndicator?>()

    override fun afterDocumentSaved(document: Document) {
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        if (file.extension != "kt" && file.extension != "java") return

        val project = ProjectLocator.getInstance().guessProjectForFile(file) ?: return
        if (project.isDisposed) return

        val hasRegistrable = document.text.lineSequence().any { line ->
            line.trim().startsWith("@QMLRegistrable")
        }

        if (!hasRegistrable) return

        scheduleKspRun(project, file)
    }

    private fun scheduleKspRun(project: Project, file: VirtualFile) {
        pendingTask.getAndSet(null)?.cancel()

        AppExecutorUtil.getAppScheduledExecutorService().schedule({
            if (project.isDisposed) return@schedule
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                runCommandWithIndicator(project, file)
            }
        }, 500, TimeUnit.MILLISECONDS)
    }

    private fun runCommandWithIndicator(project: Project, file: VirtualFile) {
        val projectPath = File(project.basePath ?: return)
        val cmd = QmlToolCommand.GradleKsp.buildCommandLine(GradleKspConfig(projectPath)) ?: run {
            logger.warn("buildCommandLine returned null for $projectPath")
            return
        }

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "QML: generating tooling files...", true) {
                override fun run(indicator: ProgressIndicator) {
                    pendingTask.set(indicator)
                    indicator.text = "Running KSP for ${file.name}..."
                    indicator.isIndeterminate = true

                    try {
                        val handler = CapturingProcessHandler(cmd)
                        val output = handler.runProcessWithProgressIndicator(indicator)

                        if (indicator.isCanceled) return

                        if (output.exitCode == 0) {
                            logger.info("KSP succeeded for ${file.name}")
                            refreshVfsAndRestartLsp(project, projectPath)
                        } else {
                            logger.warn("KSP failed for ${file.name}: (exit ${output.exitCode}) ${output.stderr}")
                        }
                    } catch (e: ProcessCanceledException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error("KSP execution error for ${file.name}", e)
                    } finally {
                        pendingTask.compareAndSet(indicator, null)
                    }
                }
            }
        )
    }
    private fun refreshVfsAndRestartLsp(project: Project, projectPath: File) {
        val generatedPath = projectPath.resolve("build/generated/ksp")
        val virtualFolder = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(generatedPath)

        if (virtualFolder != null) {
            VfsUtil.markDirtyAndRefresh(false, true, true, virtualFolder)
        } else {
            logger.warn("KSP generated folder not found at $generatedPath")
        }
        QmlLspClient.restart(project)
    }
}
