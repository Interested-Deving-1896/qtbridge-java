/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileReader

internal object QmlToolingInfoParser {
    private val logger = logger<QmlToolingInfoParser>()
    private const val FILENAME = "qmltools.json"
    private const val STANDARD_LOCATION = ".ide_tools"

    fun parse(project: Project): QmlToolingConfig? {
        val projectBasePath = project.basePath ?: run {
            logger.warn("Project base path is not available")
            return null
        }
        val configFile = File("$projectBasePath/$STANDARD_LOCATION", FILENAME)
        if (!configFile.exists()) {
            logger.warn("QML tooling config file not found: ${configFile.absolutePath}")
            return null
        }

        return try {
            FileReader(configFile).use { reader -> Gson().fromJson(reader, QmlToolingConfig::class.java) }
        } catch (e: JsonSyntaxException) {
            logger.error("Failed to parse QML tooling config file", e)
            null
        } catch (e: Exception) {
            logger.error("Error reading QML tooling config file", e)
            null
        }
    }
}
