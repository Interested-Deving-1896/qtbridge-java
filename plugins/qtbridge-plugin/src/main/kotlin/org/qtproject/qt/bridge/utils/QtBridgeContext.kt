/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils

import org.gradle.api.Project
import org.qtproject.qt.bridge.extension.QtBridgeExtension
import org.qtproject.qt.bridge.resolver.factory.QtResolverFactory

internal data class QtBridgeContext(
    val project: Project,
    val extension: QtBridgeExtension,
    val autoDownload: Boolean
) {
    val qtResolverFactory: QtResolverFactory by lazy {
        QtResolverFactory.create(
            project = project,
            autoDownload = autoDownload,
            qtLibsExtension = extension.qtLibraryPath.orNull,
            bridgeNativeExtension = extension.qtBridgeLibraryPath.orNull
        )
    }
}
