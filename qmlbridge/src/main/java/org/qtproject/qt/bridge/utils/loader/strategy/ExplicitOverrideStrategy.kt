/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.loader.strategy

import org.qtproject.qt.bridge.common.QtBridgeConstants
import java.nio.file.Files
import java.nio.file.Paths

internal class ExplicitOverrideStrategy(
    private val propertyKey: String = QtBridgeConstants.PROPERTY_NATIVE_DIR,
    private val envKey: String = QtBridgeConstants.ENV_NATIVE_DIR
) : LoadingStrategy {

    override fun tryLoad(libName: String, platformDir: String): LoadResult {
        val mappedName = System.mapLibraryName(libName)

        val propertyResult = loadFromSource(propertyKey, System.getProperty(propertyKey), mappedName)
        val envResult = loadFromSource(envKey, System.getenv(envKey), mappedName)

        // Return success if any source succeeded
        if (propertyResult is LoadResult.Success) return propertyResult
        if (envResult is LoadResult.Success) return envResult

        val reasons = listOfNotNull(
            (propertyResult as? LoadResult.Failure)?.reason,
            (envResult as? LoadResult.Failure)?.reason
        )

        return if (reasons.isNotEmpty()) {
            LoadResult.Failure(reasons.joinToString(", "))
        } else {
            LoadResult.Failure("Neither $propertyKey nor $envKey was defined; no override paths provided.")
        }
    }

    private fun loadFromSource(sourceLabel: String, nativeDir: String?, mappedName: String): LoadResult? {
        if (nativeDir.isNullOrEmpty())
            return LoadResult.Failure("No directory provided via $sourceLabel")

        val libPath = Paths.get(nativeDir, mappedName)
        return if (Files.exists(libPath)) {
            LoadResult.Success(libPath)
        } else {
            LoadResult.Failure("Library not found at $libPath (from $sourceLabel)")
        }
    }
}
