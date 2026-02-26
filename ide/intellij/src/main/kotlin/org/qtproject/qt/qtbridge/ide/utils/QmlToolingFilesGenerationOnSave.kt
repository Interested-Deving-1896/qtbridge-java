/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.utils

import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

internal class QmlToolingFilesGenerationOnSave : FileDocumentManagerListener {
    private val logger = logger<QmlToolingFilesGenerationOnSave>()

    override fun afterDocumentSaved(document: Document) {
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        if (file.extension != "kt" && file.extension != "java") return

        val project = ProjectLocator.getInstance().guessProjectForFile(file) ?: return
        if (project.isDisposed) return
        runCommandWithIndicator(project, file)
    }

    private fun runCommandWithIndicator(project: Project, file: VirtualFile) {
        val projectPath = File(project.basePath ?: return)
        val cmd = QmlToolCommand.GradleKsp.buildCommandLine(GradleKspConfig(projectPath)) ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating needed files..", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Processing ${file.name}..."

                try {
                    val handler = CapturingProcessHandler(cmd)
                    val output = handler.runProcessWithProgressIndicator(indicator)

                    if (output.exitCode == 0) {
                        logger.info("KSP Success for ${file.name}. Output: ${output.stdout}")
                    } else {
                        logger.warn("KSP Failed for ${file.name} with exit code ${output.exitCode}")
                    }
                } catch (e: Exception) {
                    logger.error("Execution Error during KSP transformation for ${file.name}", e)
                }
            }
        })
    }
}
