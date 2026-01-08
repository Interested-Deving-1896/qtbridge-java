/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.publish

import org.gradle.api.GradleException

internal enum class PublishEnvironment {
    LOCAL, STAGING, PRODUCTION;

    companion object {
        fun fromString(value: String?): PublishEnvironment {
            return when (value?.lowercase()) {
                "local" -> LOCAL
                "staging" -> STAGING
                "production" -> PRODUCTION
                null -> LOCAL
                else -> throw GradleException("Unknown publish environment: $value. Use 'local', 'staging'," +
                        " or 'production'")
            }
        }
    }
}
