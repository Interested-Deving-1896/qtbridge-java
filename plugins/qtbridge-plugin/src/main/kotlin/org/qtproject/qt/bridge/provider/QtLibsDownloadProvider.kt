/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.provider

import org.gradle.api.Project
import org.qtproject.qt.bridge.resolver.factory.QtResourceType
import org.qtproject.qt.bridge.utils.FileDownloader
import org.qtproject.qt.bridge.utils.FileExtractor
import org.qtproject.qt.bridge.utils.getDestinationDir
import org.qtproject.qt.bridge.utils.isArchive
import org.qtproject.qt.bridge.utility.Platform
import java.io.File
import kotlin.io.extension

internal class QtLibsDownloadProvider(
    project: Project,
    private val downloader: FileDownloader,
    private val extractor: FileExtractor,
    private val overwrite: Boolean = false,
) : QtProvider {
    private val destinationDir = QtResourceType.QT_LIBS.getDestinationDir(project)
    private val logger = project.logger

    override fun provide(): String {
        val platformDir = Platform.getLibraryDirectory()
        val qtLibsDir = File(destinationDir, platformDir)
        qtLibsDir.mkdirs()
        val qtLibsFolder = File(qtLibsDir, "qt/lib")
        val qtLibsFolderValid = qtLibsFolder.exists() && qtLibsFolder.isDirectory && hasLibs(qtLibsFolder)
        if (!overwrite && qtLibsFolderValid) {
            logger.info("Qt libraries found. Using existing installation at: ${qtLibsFolder.absolutePath}")
            return qtLibsFolder.absolutePath
        }
        val downloadedFile = downloader.qt().libs().to(qtLibsDir)
        var finalFile = downloadedFile
        if (downloadedFile.isArchive()) {
            finalFile = extractor.extract(downloadedFile, qtLibsDir)
            downloadedFile.delete()
        }
        return finalFile.absolutePath + "/lib"
    }
    //maybe more robust check for check if the files already exist?
    private fun hasLibs(qtLibsFolder: File): Boolean {
        return when {
            Platform.isMacOS() -> qtLibsFolder.listFiles()?.any { it.name.endsWith(".framework") } == true
            Platform.isLinux() || Platform.isWindows() -> {
                val extension = Platform.getSharedLibraryExtension()
                qtLibsFolder.listFiles()?.any { it.extension == extension } == true
            }
            else -> false
        }
    }
}
