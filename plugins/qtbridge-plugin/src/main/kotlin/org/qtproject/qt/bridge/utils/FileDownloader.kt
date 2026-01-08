/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils

import org.gradle.api.Action
import org.gradle.workers.WorkerExecutor
import org.qtproject.qt.bridge.utils.exception.QtResourceDownloadException
import org.qtproject.qt.bridge.utils.url.QtPlatformUrls
import org.qtproject.qt.bridge.utils.workers.DownloadParameters
import org.qtproject.qt.bridge.utils.workers.DownloadWorker
import java.io.File

internal class FileDownloader(private val workerExecutor: WorkerExecutor) {
    private fun download(qtResource: QtDownload.QtResourceDownload, destinationDir: File): File {
        destinationDir.mkdirs()
        val description  = qtResource.getDescription()
        val url = qtResource.getUrl()
        val fileName = url.substringAfterLast('/')
        require(fileName.isNotBlank()) { "Cannot determine filename from URL: $url" }

        val destinationFile = File(destinationDir, fileName)
        try {
            workerExecutor.noIsolation().apply {
                submit(DownloadWorker::class.java, object : Action<DownloadParameters> {
                    override fun execute(parameters: DownloadParameters) {
                        parameters.url.set(url)
                        parameters.fileName.set(fileName)
                        parameters.destination.set(destinationDir)
                        parameters.description.set(description)
                    }
                })
                await()
            }

        } catch (e: Exception) {
            throw QtResourceDownloadException(description, e.cause)
        }
        return destinationFile
    }

    fun qt(): QtDownload = QtDownload()

    internal inner class QtDownload {
        private val platformUrls by lazy { QtPlatformUrls.provideUrls() }

        fun libs() = QtResourceDownload(platformUrls.qtLibsUrl, "Qt libraries")
        fun bridgeNativeLib() = QtResourceDownload(platformUrls.nativeLibUrl, "Qt Bridge native library")

        internal inner class QtResourceDownload(
            private val url: String,
            private val resourceName: String
        ) {
            fun to(directory: File): File = download(
                qtResource = this,
                destinationDir = directory,
            )
            fun getDescription() = "$resourceName for ${platformUrls.platform}"
            fun getUrl() = url
        }
    }
}
