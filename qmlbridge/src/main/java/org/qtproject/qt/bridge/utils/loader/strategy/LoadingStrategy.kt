/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.loader.strategy

internal interface LoadingStrategy {
    /**
     * Attempts to locate the native library using this strategy.
     *
     * @param libName The library name (e.g., "nativeLibrary")
     * @param platformDir The platform directory (e.g., "linux-x64")
     * @return The load result
     */
    fun tryLoad(libName: String, platformDir: String): LoadResult
}

