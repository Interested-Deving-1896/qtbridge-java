/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.utils

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.logger

internal sealed interface QmlToolCommand<T> {
    fun buildCommandLine(config: T): GeneralCommandLine?

    object QmlLs : QmlToolCommand<QmlLSConfig> {
        private val logger = logger<QmlLs>()

        override fun buildCommandLine(config: QmlLSConfig): GeneralCommandLine? {
            if (config.qmllsPath.isBlank()) {
                logger.warn("qmlls path is missing. LSP server will not start")
                return null
            }
            if (config.buildDir.isBlank()) {
                logger.warn("buildDir is missing. LSP server will not start")
                return null
            }
            if (config.docDir.isBlank()) {
                logger.warn("docDir is missing — hover documentation hints will be unavailable")
            }

            return GeneralCommandLine().apply {
                exePath = config.qmllsPath
                addParameters("-b", config.buildDir)
                if (config.docDir.isNotBlank()) addParameters("-d", config.docDir)
            }
        }
    }

    object QmlLint : QmlToolCommand<QmlLintConfig> {
        override fun buildCommandLine(config: QmlLintConfig): GeneralCommandLine {
            return GeneralCommandLine().apply {
                exePath = config.qmllintPath
                config.qmlImportPaths.forEach { addParameters("-I", it) }
                config.qmlResourcesPaths.forEach { addParameters("-resource", it) }
                addParameters("--json", "-")
                addParameters("--bare", config.qmlFilePath)
            }
        }
    }
}
