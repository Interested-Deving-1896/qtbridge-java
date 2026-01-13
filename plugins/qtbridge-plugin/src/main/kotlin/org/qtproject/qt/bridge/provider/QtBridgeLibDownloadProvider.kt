/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.provider

import org.gradle.api.Project
import org.qtproject.qt.bridge.resolver.QtResourceType
import org.qtproject.qt.bridge.utils.FileDownloader
import org.qtproject.qt.bridge.utils.FileExtractor
import org.qtproject.qt.bridge.utils.getDestinationDir
import org.qtproject.qt.bridge.utils.getQtBridgeNativeLibName
import org.qtproject.qt.bridge.utils.isArchive
import org.qtproject.qt.bridge.utility.Platform
import java.io.File

internal class QtBridgeLibDownloadProvider(
    project: Project,
    private val downloader: FileDownloader,
    private val extractor: FileExtractor,
    private val overwrite: Boolean = false,
) : QtProvider {

    private val logger = project.logger
    private val destinationDir = QtResourceType.BRIDGE_NATIVE.getDestinationDir(project)

    override fun provide(): String {
        val platformDir = Platform.getLibraryDirectory()
        val nativeLibDir = File(destinationDir, platformDir)
        nativeLibDir.mkdirs()
        val targetFile = File(nativeLibDir, getQtBridgeNativeLibName())

        if (!overwrite && targetFile.exists()) {
            logger.lifecycle("QtBridge library detected at: ${targetFile.absolutePath}")
            // since the the libs are organized in bridge-native/platform/libQtBridge.*
            return targetFile.parentFile.parentFile.absolutePath
        }

        val downloadedFile = downloader.qt().bridgeNativeLib().to(nativeLibDir)
        var finalLibFile = downloadedFile
        if (downloadedFile.isArchive()) {
            finalLibFile = extractor.extract(downloadedFile, nativeLibDir)
            downloadedFile.delete()
            val extractedLib = finalLibFile.walkTopDown()
                .firstOrNull { it.isFile && it.extension == Platform.getSharedLibraryExtension() }
                ?: throw RuntimeException("No ${Platform.getSharedLibrarySuffix()} file found in extracted archive: ${downloadedFile.name}")
            extractedLib.copyTo(targetFile, overwrite = true)
            if (finalLibFile.isDirectory){
                finalLibFile.deleteRecursively()
            }
            finalLibFile = targetFile
        } else {
            if (downloadedFile != targetFile) {
                downloadedFile.renameTo(targetFile)
                finalLibFile = targetFile
            }
        }
        // since the the libs are organized in bridge-native/platform/libQtBridge.*
        return finalLibFile.parentFile.parentFile.absolutePath
    }
}
