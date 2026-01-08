/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.publish.config

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.getByType
import org.qtproject.qt.bridge.publish.PublishContext
import org.qtproject.qt.bridge.publish.PublishEnvironment

internal class PublishLoggerConfigurer : Configurer {

    override fun configure(context: PublishContext) {
        val project = context.project
        val environment = context.environment

        project.tasks.named("publish") {
            doFirst {
                logPublishingInfo(project, environment)
            }
            doLast {
                logger.lifecycle("Published successfully!")
            }
        }
    }

    private fun logPublishingInfo(
        project: Project,
        environment: PublishEnvironment,
    ) {
        val publishing = project.extensions.getByType<PublishingExtension>()
        val logger = project.logger

        val moduleType = when {
            project.pluginManager.hasPlugin("java-gradle-plugin") -> "Plugin"
            project.pluginManager.hasPlugin("java-library") -> "Library"
            project.pluginManager.hasPlugin("java") -> "Java (java)" // A general fallback
            else -> "Unknown/Generic"
        }
        logger.lifecycle("═".repeat(80))
        logger.lifecycle("  Publishing Artifact: ${project.group}:${project.name}:${project.version}")
        logger.lifecycle("  Module Type: $moduleType")
        logger.lifecycle("  Environment: $environment")
        when (environment) {
            PublishEnvironment.LOCAL -> logger.lifecycle("  Repository: Maven Local (~/.m2/repository)")
            else -> logger.lifecycle("  Repository: ${publishing.repositories.names}")
        }
        logger.lifecycle("═".repeat(80))
    }
}
