/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.url

import org.qtproject.qt.bridge.utility.Platform
import org.qtproject.qtbridge.PluginVersion

internal class QtPlatformUrlBuilder private constructor(private val baseUrl: String) {
    private val suffix = Platform.getLibraryDirectory()
    private val version = PluginVersion.VERSION
    val qtLibsFile: String
        get() = buildFileName(QT_MINIMAL_NAME)

    val qtBridgeNativeFile: String
        get() = buildFileName(QT_BRIDGE_NATIVE_NAME)

    private fun buildFileName(prefix: String): String {
        return when {
            Platform.isMacOS() || Platform.isLinux() ->
                "${prefix}_${suffix}_${version}.tar.gz"
            else -> error("Platform not supported at the moment!")
        }
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
        private const val QT_MINIMAL_NAME= "qt_minimal"
        private const val QT_BRIDGE_NATIVE_NAME = "java_qtBridge"
        fun default(): QtPlatformUrlBuilder = QtPlatformUrlBuilder(DEFAULT_BASE_URL)
    }
}
