/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.url

import org.qtproject.qt.bridge.utility.Platform
import org.qtproject.qt.bridge.utils.QtBridgeResolverUtils

internal class QtPlatformUrlBuilder private constructor(private val baseUrl: String) {
    private val version = QtBridgeResolverUtils.fullVersion()
    val qtLibsFile: String
        get() = buildQtLibsFileName()

    val qtBridgeNativeFile: String
        get() = buildQtBridgeNativeFileName()

    private fun buildQtLibsFileName(): String {
        return "${QT_LIBRARY_ARCHIVE_PREFIX}_${qtLibsPlatformSuffix()}_${version}.${qtLibsArchiveExtension()}"
    }

    private fun buildQtBridgeNativeFileName(): String {
        return "${QT_BRIDGE_ARCHIVE_PREFIX}_${qtBridgeNativePlatformSuffix()}_${version}.tar.gz"
    }

    private fun qtLibsPlatformSuffix(): String {
        return when {
            Platform.isMacOS() -> "macos_universal"
            Platform.isLinux() && Platform.isAarch64() -> "linux_aarch64"
            Platform.isLinux() && Platform.isX86_64() -> "linux_x86_64"
            Platform.isWindows() && Platform.isAarch64() -> "win_arm64"
            Platform.isWindows() && Platform.isX86_64() -> "win_x86_64"
            else -> error("Qt library packages are not available for ${Platform.getDescription()}")
        }
    }

    private fun qtBridgeNativePlatformSuffix(): String {
        return when {
            Platform.isMacOS() -> "macos_universal"
            Platform.isLinux() && Platform.isAarch64() -> "linux_aarch64"
            Platform.isLinux() && Platform.isX86_64() -> "linux_x86_64"
            Platform.isWindows() && Platform.isAarch64() -> "win_arm64"
            Platform.isWindows() && Platform.isX86_64() -> "win_amd64"
            else -> error("Qt Bridge native packages are not available for ${Platform.getDescription()}")
        }
    }

    private fun qtLibsArchiveExtension(): String {
        return if (Platform.isWindows()) "zip" else "tar.gz"
    }

    fun build(): QtPlatformUrls {
        val qtLibsUrl = "$baseUrl/$qtLibsFile"
        val nativeLibUrl = "$baseUrl/$qtBridgeNativeFile"
        return QtPlatformUrls(
            qtLibsUrl = qtLibsUrl,
            nativeLibUrl = nativeLibUrl,
            platform = Platform.getDescription()
        )
    }

    companion object {
        private const val DEFAULT_BASE_URL = "https://download.qt.io/snapshots/ci/qt"
        private const val QT_LIBRARY_ARCHIVE_PREFIX= "qt_minimal"
        private const val QT_BRIDGE_ARCHIVE_PREFIX = "java_qtBridge"
        fun default(): QtPlatformUrlBuilder = QtPlatformUrlBuilder(DEFAULT_BASE_URL)
    }
}
