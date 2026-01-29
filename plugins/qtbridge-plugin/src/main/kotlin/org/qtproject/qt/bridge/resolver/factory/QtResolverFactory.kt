/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.resolver.factory

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.extensions.core.serviceOf
import org.gradle.workers.WorkerExecutor
import org.qtproject.qt.bridge.provider.QtBridgeLibDownloadProvider
import org.qtproject.qt.bridge.provider.QtLibsDownloadProvider
import org.qtproject.qt.bridge.provider.QtProvider
import org.qtproject.qt.bridge.resolver.*
import org.qtproject.qt.bridge.utils.FileDownloader
import org.qtproject.qt.bridge.utils.FileExtractor

internal class QtResolverFactory(
    private val logger: Logger,
    private val qtLibsExtension: String?,
    private val bridgeNativeExtension: String?,
    private val libsProvider: QtProvider? = null,
    private val bridgeNativeProvider: QtProvider? = null,
) {
    val libsLocationResolver by lazy {
        QtResourceLocationResolver(QtResourceType.QT_LIBS, logger, libsProvider, qtLibsExtension)
    }

    val bridgeNativeLocationResolver by lazy {
        QtResourceLocationResolver(QtResourceType.BRIDGE_NATIVE, logger, bridgeNativeProvider, bridgeNativeExtension)
    }

    private val qtRootResolver by lazy {
        val explicitPathResolver = QtResourceLocationResolver(
            QtResourceType.QT_ROOT, logger, null, qtLibsExtension
        )
        QtRootResolver(logger, explicitPathResolver, libsLocationResolver)
    }

    val qmllsResolver by lazy { QmlToolResolver(QmlTool.QMLLS, qtRootResolver, logger) }

    val qmllintResolver by lazy { QmlToolResolver(QmlTool.QMLLINT, qtRootResolver, logger) }

    val qmlTypeRegistrarResolver by lazy { QmlToolResolver(QmlTool.QMLTYPEREGISTRAR, qtRootResolver, logger) }

    val qtDocDirResolver by lazy { QtSubdirResolver(qtRootResolver, "doc")}

    val qmlImportDirResolver by lazy { QtSubdirResolver(qtRootResolver, "qml") }

    companion object {
        fun createDefault(
            project: Project,
            qtLibsExtension: String?,
            bridgeNativeExtension: String?,
        ): QtResolverFactory {
            return QtResolverFactory(
                logger = project.logger,
                qtLibsExtension = qtLibsExtension,
                bridgeNativeExtension = bridgeNativeExtension,
            )
        }
        fun createWithDownloadProviders(
            project: Project,
            qtLibsExtension: String?,
            bridgeNativeExtension: String?,
        ): QtResolverFactory {
            val workerExecutor: WorkerExecutor = project.serviceOf()
            val downloader = FileDownloader(workerExecutor)
            val extractor = FileExtractor(workerExecutor)

            return QtResolverFactory(
                logger = project.logger,
                qtLibsExtension = qtLibsExtension,
                bridgeNativeExtension = bridgeNativeExtension,
                libsProvider = QtLibsDownloadProvider(project, downloader, extractor),
                bridgeNativeProvider = QtBridgeLibDownloadProvider(project, downloader, extractor)
            )
        }
    }
}
