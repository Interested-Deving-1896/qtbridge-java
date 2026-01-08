/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.resolver

import org.gradle.api.Project
import org.gradle.internal.extensions.core.serviceOf
import org.gradle.workers.WorkerExecutor
import org.qtproject.qt.bridge.provider.QtBridgeLibProvider
import org.qtproject.qt.bridge.provider.QtLibsProvider
import org.qtproject.qt.bridge.provider.QtProvider
import org.qtproject.qt.bridge.utils.FileDownloader
import org.qtproject.qt.bridge.utils.FileExtractor

internal class QtPathResolverFactory(
    private val project: Project,
    private val providers: Map<QtResourceType, QtProvider>
) {
    private val cache = mutableMapOf<QtResourceType, QtPathResolver>()

    fun get(type: QtResourceType): QtPathResolver {
        return cache.getOrPut(type) {
            val provider = providers[type] ?: error("No QtProvider registered for $type")
            QtPathResolver(project, provider, type)
        }
    }

    val libsResolver get() = get(QtResourceType.LIBS)
    val bridgeNativeResolver get() = get(QtResourceType.BRIDGE_NATIVE)

    companion object {
        fun default(project: Project): QtPathResolverFactory {
            val workerExecutor: WorkerExecutor = project.serviceOf()
            val downloader = FileDownloader(workerExecutor)
            val extractor = FileExtractor(workerExecutor)
            val providers = mapOf(
                QtResourceType.LIBS to QtLibsProvider(project, downloader, extractor),
                QtResourceType.BRIDGE_NATIVE to QtBridgeLibProvider(project, downloader, extractor)
            )
            return QtPathResolverFactory(project, providers)
        }
    }
}
