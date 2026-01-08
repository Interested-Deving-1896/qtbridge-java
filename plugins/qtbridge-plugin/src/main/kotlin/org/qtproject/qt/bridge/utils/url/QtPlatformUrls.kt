/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.url

internal class QtPlatformUrls(
    val qtLibsUrl: String,
    val nativeLibUrl: String,
    val platform: String
) {
    companion object {
        fun provideUrls(): QtPlatformUrls = QtPlatformUrlBuilder.default().build()
    }
}
