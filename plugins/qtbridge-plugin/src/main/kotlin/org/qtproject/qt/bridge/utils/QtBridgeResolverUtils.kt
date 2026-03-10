/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils

import org.qtproject.qt.bridge.common.QtBridgeConstants
import org.qtproject.qt.bridge.utility.Platform
import org.qtproject.qtbridge.PluginVersion

internal object QtBridgeResolverUtils {

    fun nativeLibName(): String {
        val prefix = if (Platform.isWindows()) "" else "lib"
        val suffix = Platform.getSharedLibrarySuffix()
        return "${prefix}${QtBridgeConstants.NATIVE_LIB_NAME}$suffix"
    }

    fun majorMinorVersion(): String {
        return PluginVersion.VERSION
            .split('.')
            .take(2)
            .joinToString(".")
    }

    fun fullVersion(): String {
        return PluginVersion.VERSION
    }
}
