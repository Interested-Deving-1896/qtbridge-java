/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.workers

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.jetbrains.kotlin.org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.jetbrains.kotlin.org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.qtproject.qt.bridge.resolver.factory.QmlTool
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

internal interface ExtractionParameters : WorkParameters {
    val archive: RegularFileProperty
    val destination: RegularFileProperty
}

internal abstract class ExtractionWorker : WorkAction<ExtractionParameters> {
    private val logger = Logging.getLogger(ExtractionWorker::class.java)

    override fun execute() {
        val archive = parameters.archive.asFile.get()
        val destination = parameters.destination.asFile.get()
        logger.lifecycle("Extracting ${archive.name} ...")
        try {
            when {
                archive.name.endsWith(".zip") -> extractZip(archive, destination)
                archive.name.endsWith(".tar.gz") || archive.name.endsWith(".tgz") -> extractTarGz(archive, destination)
                archive.name.endsWith(".tar") -> extractTar(archive, destination)
                else -> throw IllegalArgumentException("Unsupported archive format: ${archive.name}")
            }
            logger.lifecycle("Extraction complete: ${destination.absolutePath}")
        } catch (e: Exception) {
            throw GradleException("Failed to extract ${archive.name}", e)
        }
    }

    private fun extractZip(archive: File, destination: File) {
        ZipFile(archive).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val outputFile = File(destination, entry.name)
                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }

    private fun extractTarGz(archive: File, destination: File) {
        FileInputStream(archive).use { fileIn ->
            GzipCompressorInputStream(BufferedInputStream(fileIn)).use { gzipIn ->
                TarArchiveInputStream(gzipIn).use { tarIn ->
                    extractTarEntries(tarIn, destination)
                }
            }
        }
    }

    private fun extractTar(archive: File, destination: File) {
        FileInputStream(archive).use { fileIn ->
            TarArchiveInputStream(BufferedInputStream(fileIn)).use { tarIn ->
                extractTarEntries(tarIn, destination)
            }
        }
    }

    private fun extractTarEntries(tarIn: TarArchiveInputStream, destination: File) {
        generateSequence { tarIn.nextEntry }.forEach { entry ->
            val outputFile = File(destination, entry.name)

            when {
                entry.isDirectory -> outputFile.mkdirs()
                entry.linkFlag == TarArchiveEntry.LF_SYMLINK -> {
                    outputFile.parentFile?.mkdirs()
                    val linkTarget = entry.linkName
                    try {
                        Files.createSymbolicLink(outputFile.toPath(), Paths.get(linkTarget))
                    } catch (e: UnsupportedOperationException) {
                        throw GradleException("Filesystem does not support symlinks: $outputFile", e)
                    }
                }

                else -> {
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { out ->
                        tarIn.copyTo(out)
                    }
                    if (shouldBeExecutable(entry.name)) {
                        try {
                            outputFile.setExecutable(true, false)
                        } catch (e: SecurityException) {
                            logger.warn("Failed to set executable permission for ${outputFile.name}: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    // check the extracted file is one of the QML tools that needs executable permission
    private fun shouldBeExecutable(entryName: String): Boolean {
        return QmlTool.values().any { tool ->
            entryName.endsWith("${tool.subDirectory}/${tool.toolName}")
        }
    }
}
