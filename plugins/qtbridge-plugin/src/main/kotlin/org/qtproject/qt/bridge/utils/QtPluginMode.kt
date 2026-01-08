/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils

internal enum class QtPluginMode {
    DEV, NON_DEV;

    companion object {
        fun fromString(value: String?): QtPluginMode =
            when (value?.lowercase()) {
                "nondev", "non-dev" -> NON_DEV
                else -> DEV
            }
    }
}
