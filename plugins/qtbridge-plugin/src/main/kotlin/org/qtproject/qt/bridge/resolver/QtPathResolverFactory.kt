/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.resolver

import org.gradle.api.Project
import org.gradle.internal.extensions.core.serviceOf
import org.gradle.workers.WorkerExecutor
import org.qtproject.qt.bridge.provider.QtBridgeLibDownloadProvider
import org.qtproject.qt.bridge.provider.QtLibsDownloadProvider
import org.qtproject.qt.bridge.provider.QtProvider
import org.qtproject.qt.bridge.utils.FileDownloader
import org.qtproject.qt.bridge.utils.FileExtractor

internal class QtPathResolverFactory(
    private val project: Project,
    private val providers: Map<QtResourceType, QtProvider>?
)  {
    private val cache = mutableMapOf<QtResourceType, QtPathResolver>()

    fun get(type: QtResourceType): QtPathResolver = cache.getOrPut(type) {
        val provider = providers?.let {
            it[type] ?: error("No QtProvider registered for $type")
        }
        QtPathResolver(project, type, provider)
    }

    val libsResolver get() = get(QtResourceType.LIBS)
    val bridgeNativeResolver get() = get(QtResourceType.BRIDGE_NATIVE)

    companion object {
        fun default(project: Project): QtPathResolverFactory {
            val workerExecutor: WorkerExecutor = project.serviceOf()
            val downloader = FileDownloader(workerExecutor)
            val extractor = FileExtractor(workerExecutor)
            val providers = mapOf(
                QtResourceType.LIBS to QtLibsDownloadProvider(project, downloader, extractor),
                QtResourceType.BRIDGE_NATIVE to QtBridgeLibDownloadProvider(project, downloader, extractor)
            )
            return QtPathResolverFactory(project, providers)
        }

        fun withoutDownloadProviders(project: Project) = QtPathResolverFactory(project, null)
    }
}
