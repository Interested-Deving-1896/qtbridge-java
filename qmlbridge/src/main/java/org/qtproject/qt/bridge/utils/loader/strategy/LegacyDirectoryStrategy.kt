/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.loader.strategy

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

/**
 * Strategy primarily for development environments. It searches for a "project root"
 * based on the presence of a marker directory ('legacyDir').
 */
internal class LegacyDirectoryStrategy(private val legacyDir: String) : LoadingStrategy {
    override fun tryLoad(libName: String, platformDir: String): LoadResult {
        val mappedName = System.mapLibraryName(libName)
        val libPath = findProjectRoot()
            .resolve(legacyDir)
            .resolve("libs")
            .resolve(platformDir)
            .resolve(mappedName)

        return if (libPath.exists()) {
            LoadResult.Success(libPath)
        } else {
            LoadResult.Failure(libPath.toString())
        }
    }

    private fun findProjectRoot(): Path {
        var current: Path? = try {
            val uri = javaClass.protectionDomain.codeSource.location.toURI()
            Paths.get(uri)
        } catch (e: Exception) {
            Paths.get(".")
        }

        if (current != null && Files.isRegularFile(current)) {
            current = current.parent
        }
        while (current != null) {
            if (Files.exists(current.resolve(legacyDir))) {
                return current
            }
            val parent = current.parent
            if (parent == current) break
            current = parent
        }
        return Paths.get(".")
    }
}
