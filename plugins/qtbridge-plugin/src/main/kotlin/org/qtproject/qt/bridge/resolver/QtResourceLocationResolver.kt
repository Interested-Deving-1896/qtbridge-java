/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.resolver

import org.gradle.api.logging.Logger
import org.qtproject.qt.bridge.provider.QtProvider
import org.qtproject.qt.bridge.resolver.factory.QtResourceType
import org.qtproject.qt.bridge.utils.isValidQtRoot
import java.io.File

internal class QtResourceLocationResolver(
    private val resourceType: QtResourceType,
    private val logger: Logger,
    private val provider: QtProvider?,
    private val extensionResourcePath: String?
) : QtFileResolver {

    override fun resolve(): File? {
        val description = resourceType.description

        extensionResourcePath?.let {
            logger.info("Using $description from extension: $it")
            return File(it)
        }

        resourceType.propertyKey
            ?.let { System.getProperty(it) }
            ?.let {
                logger.info("Using $description from project property: $it")
                return File(it)
            }
        resourceType.envKey
            ?.let { System.getenv(it) }
            ?.let {
                logger.info("Using $description from environment: $it")
                return File(it)
            }
        resolveFromPath()?.let {
            logger.info("Using $description resolved from PATH: $it")
            return File(it)
        }

        provider?.let {
            return File(it.provide())
        }
        return null
    }

    private fun resolveFromPath(): String? {
        if (resourceType != QtResourceType.QT_ROOT)
            return null

        val pathDirs = System.getenv("PATH")
            ?.split(File.pathSeparator)
        pathDirs?.forEach { pathDir ->
            findQtRoot(File(pathDir))?.let {
                return it.absolutePath
            }
        }
        return null
    }

    private fun findQtRoot(directory: File) = when {
        // directory is already Qt root
        directory.isValidQtRoot() -> directory
        // directory is a bin folder
        isLikelyQtBinDirectory(directory) -> directory.parentFile?.takeIf { it.isValidQtRoot() }
        else -> null
    }

    private fun isLikelyQtBinDirectory(directory: File): Boolean {
        if (!directory.name.equals("bin", ignoreCase = true))
            return false
        return listOf("qmake", "qmake.exe", "qmake6", "qmake6.exe")
            .any { directory.resolve(it).exists() }
    }
}
