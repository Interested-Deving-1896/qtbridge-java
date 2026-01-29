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
    val mode: QtPluginMode
) {
    val includedBridgeBuild by lazy {
        project.gradle.includedBuilds.find { it.name == "qmlbridge" }
    }

    val isDevMode: Boolean by lazy {
        mode == QtPluginMode.DEV && includedBridgeBuild != null
    }

    val qtResolverFactory: QtResolverFactory by lazy {
        if (isDevMode) {
            QtResolverFactory.createDefault(
                project = project,
                qtLibsExtension = extension.qtLibraryPath.orNull,
                bridgeNativeExtension = extension.qtBridgeLibraryPath.orNull
            )
        } else {
            QtResolverFactory.createWithDownloadProviders(
                project = project,
                qtLibsExtension = extension.qtLibraryPath.orNull,
                bridgeNativeExtension = extension.qtBridgeLibraryPath.orNull
            )
        }
    }
}
