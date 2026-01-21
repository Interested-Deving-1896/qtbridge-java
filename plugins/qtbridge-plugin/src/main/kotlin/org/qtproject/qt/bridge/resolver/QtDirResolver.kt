/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.resolver

import org.gradle.api.logging.Logger
import org.qtproject.qt.bridge.utils.isValidQtRoot
import java.io.File

internal class QtSubdirResolver(private val rootResolver: QtFileResolver, private val relativePath: String) : QtFileResolver {
    override fun resolve(): File? {
        val qtRoot = rootResolver.resolve() ?: return null
        val dir = qtRoot.resolve(relativePath)
        return dir.takeIf { it.exists() && dir.isDirectory }
    }
}

internal class QtRootResolver(
    private val logger: Logger,
    private val qtRootLocationResolver: QtResourceLocationResolver,
    private val qtLibsLocationResolver: QtResourceLocationResolver
) : QtFileResolver {
    private var qtRootCache: File? = null

    override fun resolve(): File? {
        qtRootCache?.let { return it }

        resolveQtRootDirect()?.let {
            logger.info("Qt root resolved from explicit configuration: $it")
            qtRootCache = it
            return it
        }

        logger.info("Qt root not explicitly configured. Attempting to derive from Qt libraries.")
        val derived = resolveQtRootFromLibs()

        if (derived == null) {
            logger.info(
                "Unable to resolve Qt root directory. Please configure Qt using one of these methods:\n" +
                        "  1. Set environment variable: QTBRIDGE_QTDIR=<qt-installation-path>\n" +
                        "  2. Add Qt installation to PATH\n" +
                        "  3. Configure qtBridge.qtLibraryPath in QtBridge plugin settings"
            )
            return null
        }

        logger.info("Qt root successfully derived from libraries: $derived")
        qtRootCache = derived
        return derived
    }

    private fun resolveQtRootDirect() = qtRootLocationResolver.resolve().asValidQtRoot()
    private fun resolveQtRootFromLibs() = qtLibsLocationResolver.resolve()?.parentFile.asValidQtRoot()
    private fun File?.asValidQtRoot() = this?.takeIf { it.exists() && it.isValidQtRoot() }
}
