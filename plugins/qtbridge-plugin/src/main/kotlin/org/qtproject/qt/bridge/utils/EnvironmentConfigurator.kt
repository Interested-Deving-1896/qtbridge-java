/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils

import org.gradle.api.logging.Logger
import org.gradle.api.tasks.JavaExec
import org.qtproject.qt.bridge.common.QtBridgeConstants
import org.qtproject.qt.bridge.utility.Platform
import java.io.File

internal class EnvironmentConfigurator(
    private val logger: Logger,
    private val qtLibsDirPath: String,
    private val qtBinDirPath: String,
    private val qtBridgeLibraryPath: String
) {
    fun configure(task: JavaExec) {
        val libDir = File(qtBridgeLibraryPath, Platform.getLibraryDirectory())
        val libFile = File(libDir, QtBridgeResolverUtils.nativeLibName())
        //remove RPATHs on macOS to avoid linking issues
        if (Platform.isMacOS() && libFile.exists()) {
            removeRPaths(libFile)
        }
        //set it in the Gradle JVM too
        System.setProperty(QtBridgeConstants.PROPERTY_NATIVE_DIR, libDir.absolutePath)
        //pass to child JVM process when JavaExec task runs
        task.systemProperty(QtBridgeConstants.PROPERTY_NATIVE_DIR,libDir.absolutePath)
        configurePath(task)
        configureLibraryPath(task)
    }

    private fun configurePath(task: JavaExec) {
        val currentPath = System.getenv("PATH") ?: ""
        val newPath = if (Platform.isWindows()) {
            // On Windows the libraries are typically in bin folder, not lib
            buildPathString(qtBinDirPath, qtLibsDirPath, currentPath)
        } else {
            buildPathString(qtLibsDirPath, currentPath)
        }
        task.environment("PATH", newPath)
    }

    private fun configureLibraryPath(task: JavaExec) {
        when {
            Platform.isMacOS() -> {
                val currentDyldPath = System.getenv("DYLD_FRAMEWORK_PATH") ?: ""
                val newDyldPath = buildPathString(qtLibsDirPath, currentDyldPath)
                task.environment("DYLD_FRAMEWORK_PATH", newDyldPath)
            }
            Platform.isLinux() -> {
                val currentLdPath = System.getenv("LD_LIBRARY_PATH") ?: ""
                val newLdPath = buildPathString(qtLibsDirPath, currentLdPath)
                task.environment("LD_LIBRARY_PATH", newLdPath)
            }
        }
    }

    private fun buildPathString(vararg paths: String): String = buildString {
        paths.forEachIndexed { index, path ->
            if (path.isNotEmpty()) {
                if (index > 0 && isNotEmpty()) append(File.pathSeparator)
                append(path)
            }
        }
    }

    private fun removeRPaths(libFile: File) {
        try {
            //check if the library has any RPATHs
            val otoolProcess = ProcessBuilder("otool", "-l", libFile.absolutePath)
                .redirectErrorStream(true)
                .start()

            val output = otoolProcess.inputStream.bufferedReader().use { it.readText() }
            val otoolExit = otoolProcess.waitFor()

            if (otoolExit != 0) {
                logger.info("otool failed with exit code $otoolExit for ${libFile.name}")
                return
            }

            //extract RPATHs from otool output
            val rpaths = Regex("path (.+?) \\(offset").findAll(output)
                .map { it.groupValues[1].trim() }
                .toList()

            if (rpaths.isEmpty()) {
                logger.info("No RPATHs found in ${libFile.name}")
                return
            }
            logger.info("Found ${rpaths.size} RPATH(s) in ${libFile.name}")

            //remove RPATHs
            for (rpath in rpaths) {
                val deleteProcess = ProcessBuilder(
                    "install_name_tool",
                    "-delete_rpath",
                    rpath,
                    libFile.absolutePath
                ).redirectErrorStream(true).start()

                val deleteExit = deleteProcess.waitFor()
                if (deleteExit == 0) {
                    logger.info("Removed RPATH: $rpath")
                } else {
                    val errorOutput = deleteProcess.inputStream.bufferedReader().use { it.readText() }
                    logger.info("Failed to remove RPATH: $rpath (exit $deleteExit)")
                    if (errorOutput.isNotBlank()) {
                        logger.info("Error details: $errorOutput")
                    }
                }
            }
        } catch (e: Exception) {
            logger.info("Failed to process RPATHs in ${libFile.name}: ${e.message}")
        }
    }
}
