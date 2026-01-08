/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils

import org.gradle.api.Action
import org.gradle.workers.WorkerExecutor
import org.qtproject.qt.bridge.utils.exception.QtResourceExtractionException
import org.qtproject.qt.bridge.utils.workers.ExtractionParameters
import org.qtproject.qt.bridge.utils.workers.ExtractionWorker
import java.io.File

internal class FileExtractor(private val workerExecutor: WorkerExecutor) {
    fun extract(archiveFile: File, destinationDir: File): File {
        require(archiveFile.exists()) {
            "Archive file does not exist: ${archiveFile.absolutePath}"
        }
        destinationDir.mkdirs()
        try {
            workerExecutor.noIsolation().apply {
                submit(ExtractionWorker::class.java, object : Action<ExtractionParameters> {
                    override fun execute(params: ExtractionParameters) {
                        params.archive.set(archiveFile)
                        params.destination.set(destinationDir)
                    }
                })
                await()
            }
        } catch (e: Exception) {
            throw QtResourceExtractionException(archiveFile, e.cause)
        }

        val extractedFiles = destinationDir.listFiles()
        require(!extractedFiles.isNullOrEmpty()) {
            "Extraction failed: no files found in ${destinationDir.absolutePath}"
        }
        return extractedFiles.singleOrNull { it.isDirectory } ?: destinationDir
    }
}
