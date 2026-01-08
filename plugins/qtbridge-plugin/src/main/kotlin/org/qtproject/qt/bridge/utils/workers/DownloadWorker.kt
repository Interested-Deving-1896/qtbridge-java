/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.workers

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.logging.Logging
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.net.URI

interface DownloadParameters : WorkParameters {
    val url: Property<String>
    val fileName: Property<String>
    val destination: RegularFileProperty
    val description: Property<String>
}

internal abstract class DownloadWorker : WorkAction<DownloadParameters> {

    private val logger = Logging.getLogger(DownloadWorker::class.java)

    override fun execute() {
        val url = parameters.url.get()
        val destinationDir = parameters.destination.asFile.get()
        val description = parameters.description.get()
        val fileName = parameters.fileName.get()
        destinationDir.mkdirs()
        val destinationFile = File(destinationDir, fileName)

        logger.lifecycle("Downloading $description...")
        try {
            val uri = URI(url)
            uri.toURL().openStream().use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            logger.lifecycle("Downloaded $description successfully: ${destinationFile.absolutePath}")
        } catch (e: Exception) {
            destinationFile.delete()
            throw GradleException("Failed to download $description from $url", e)
        }
    }
}
