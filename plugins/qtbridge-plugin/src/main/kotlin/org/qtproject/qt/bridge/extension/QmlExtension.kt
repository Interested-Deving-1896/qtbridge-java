/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.extension

import org.gradle.api.Project
import java.io.File

open class QmlExtension(private val project: Project) {
    val entryPoint = project.objects.property(String::class.java)
    val sourcePath = project.objects.property(String::class.java).convention("src/main/qml")
    val importPaths = project.objects.fileCollection()

    // Enable QML import tracing (prints module resolution)
    val importTrace = project.objects.property(Boolean::class.java).convention(false)

    internal fun resolveMainQmlFile(): File? {
        val allSourceQml = getAllSourceQmlFiles()

        if (entryPoint.isPresent) {
            val mainPath = entryPoint.get()
            val mainFile = project.file(mainPath)

            if (mainFile.exists())
                return mainFile

            // not an absolute path --> search in collected QML files
            val found = allSourceQml.firstOrNull { it.name == File(mainPath).name }
            return if (found != null) {
                project.logger.info("Found main QML file: ${found.absolutePath}")
                found
            } else {
                project.logger.warn("Could not find '$mainPath' in any source directory")
                null
            }
        }

        // auto-discovery
        val autoCandidates = setOf("Main.qml", "main.qml")

        val discovered = allSourceQml.firstOrNull { it.name in autoCandidates }
        return if (discovered != null) {
            project.logger.info("Auto-discovered main QML file: ${discovered.absolutePath}")
            discovered
        } else {
            project.logger.warn("Could not find Main.qml or main.qml in source directories")
            null
        }
    }

    internal fun resolveSourceDirectory(): File? {
        val resolvedDir = project.file(sourcePath)

        return when {
            !resolvedDir.exists() -> {
                project.logger.warn("QML source directory does not exist: ${resolvedDir.absolutePath}")
                null
            }
            !resolvedDir.isDirectory -> {
                project.logger.lifecycle("Ignoring ${resolvedDir.absolutePath} as it is not a directory")
                null
            }
            else -> resolvedDir
        }
    }

    internal fun resolveImportPaths(): List<File> {
        return importPaths.mapNotNull { rawPath ->
            val resolved = project.file(rawPath).canonicalFile

            when {
                !resolved.exists() -> {
                    project.logger.warn("QML import path does not exist: ${resolved.absolutePath}")
                    null
                }
                else -> resolved
            }
        }
    }


    private fun getAllSourceQmlFiles(): List<File> {
        val sourcePath = resolveSourceDirectory() ?: return emptyList()
        return project.fileTree(sourcePath) {
            include("**/*.qml")
        }.files.toList()

    }
}
