/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.resolver

import org.gradle.api.Project
import org.qtproject.qt.bridge.provider.QtProvider

internal class QtPathResolver(
    private val project: Project,
    private val provider: QtProvider,
    private val resourceType: QtResourceType
) {
    fun resolve(extensionValue: String?): String { // this is passed from Plugin extension (qtlibs or qtBridgeNative)
        val desc = resourceType.description
        val property: String? = resourceType.propertyKey
        val env: String? = resourceType.envKey
        val logger = project.logger

        extensionValue?.let { extVal ->
            logger.info("Using $desc from extension: $extVal")
            return extVal
        }

        if (property != null) {
            System.getProperty(property)?.let { prop ->
                logger.info("Using $desc from project property: $prop")
                return prop
            }
        }

        if (env != null) {
            System.getenv(env)?.let { envVar ->
                logger.info("Using $desc from environment: $envVar")
                return envVar
            }
        }

        logger.lifecycle("$desc not configured. Starting download and setup...")
        return provider.provide()
    }
}
