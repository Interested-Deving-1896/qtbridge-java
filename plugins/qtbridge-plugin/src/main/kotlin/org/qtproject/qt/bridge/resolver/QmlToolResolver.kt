/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.resolver

import org.gradle.api.logging.Logger
import org.qtproject.qt.bridge.resolver.factory.QmlTool
import java.io.File

internal class QmlToolResolver(
    private val qmlTool: QmlTool,
    private val rootResolver: QtRootResolver,
    private val logger: Logger
) : QtFileResolver {
    override fun resolve() = resolveExecutable(qmlTool)

    private fun resolveExecutable(qmlTool: QmlTool): File? {

        val qtRootDir = rootResolver.resolve() ?: return null

        val file = qtRootDir
            .resolve(qmlTool.subDirectory)
            .resolve(qmlTool.toolName)

        if (!file.exists()) {
            logger.warn("Qt executable '${qmlTool.toolName}' not found at ${file.absolutePath}")
            return null
        }

        if (!file.canExecute()) {
            logger.warn("Qt executable '${qmlTool.toolName}' is not executable: ${file.absolutePath}")
            return null
        }
        return file
    }
}
