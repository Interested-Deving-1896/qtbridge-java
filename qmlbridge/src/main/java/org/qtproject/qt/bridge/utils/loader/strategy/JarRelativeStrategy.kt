/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.loader.strategy

import org.qtproject.qt.bridge.utility.ClassLocationResolver
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

internal class JarRelativeStrategy(private val referenceClass: Class<*>) : LoadingStrategy {
    override fun tryLoad(libName: String, platformDir: String): LoadResult {
        val classLocationFile = ClassLocationResolver.getClassLocation(referenceClass)
            ?: return LoadResult.Failure("Could not determine class location for ${referenceClass.name}")
        val startPath = classLocationFile.toAbsolutePath()
        val mappedName = System.mapLibraryName(libName)

        if (Files.isDirectory(startPath)) {
            // DEV MODE HANDLING:
            // If we are in a 'classes' dir, we usually need to go up to the project root.
            // Heuristic: If path ends in /classes/kotlin/main or /classes/java/main, go up 3-4 levels.
            // Or simply check if 'libs' exists here; if not, walk up parents.
            val searchRoot = findProjectRootFromClasses(startPath) ?: startPath
            val localLibPath = searchRoot.resolve("libs")
                .resolve(platformDir)
                .resolve(mappedName)
            if (localLibPath.exists()) {
                return LoadResult.Success(localLibPath)
            }
        }

        // PRODUCTION MODE (JAR):
        // Use the folder containing the JAR
        val resourcePath = "/libs/$platformDir/$mappedName"
        val inputStream = referenceClass.getResourceAsStream(resourcePath)
            ?: return LoadResult.Failure("Library not found on disk or inside JAR at '$resourcePath' (Base: $startPath)")

        return try {
            val tempFile = Files.createTempFile("qtbridge_native_", ".tmp")
            tempFile.toFile().deleteOnExit() //clean on exit
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING)
            inputStream.close()
            LoadResult.Success(tempFile)
        } catch (e: Exception) {
            LoadResult.Failure("Failed to extract library from JAR: ${e.message}")
        }
    }
    private fun findProjectRootFromClasses(classesDir: Path): Path? {
        var current: Path? = classesDir
        var depth = 0
        while (current != null && depth < 5) {
            if (current.resolve("libs").exists()) {
                return current
            }
            current = current.parent
            depth++
        }
        return null
    }
}
