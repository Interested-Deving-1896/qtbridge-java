/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.resolver

import org.qtproject.qt.bridge.common.QtBridgeConstants

internal enum class QtResourceType(
    val description: String,
    val propertyKey: String?= null,
    val envKey: String? = null,
    val relativeDestination: String
) {
    LIBS(
        description = "Qt Libraries",
        relativeDestination = "qt/libs"
    ),
    BRIDGE_NATIVE(
        description = "Qt Bridge Native Library",
        propertyKey = QtBridgeConstants.PROPERTY_NATIVE_DIR,
        envKey = QtBridgeConstants.ENV_NATIVE_DIR,
        relativeDestination = "qt/bridge-native"
    )
}
