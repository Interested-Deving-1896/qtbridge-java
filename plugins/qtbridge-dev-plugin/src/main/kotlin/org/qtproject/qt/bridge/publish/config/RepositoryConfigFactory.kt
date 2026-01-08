/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.publish.config

import org.gradle.api.Project
import org.qtproject.qt.bridge.publish.PublishEnvironment
import java.net.URI

internal object RepositoryConfigFactory {
    fun from(project: Project, env: PublishEnvironment): RepositoryConfig = when (env) {
        PublishEnvironment.LOCAL -> RepositoryConfig(
            name = "Local",
            url = URI("file://${System.getProperty("user.home")}/.m2/repository")
        )

        PublishEnvironment.STAGING -> {
            val version = project.version.toString()
            val isSnapshot = version.endsWith("-SNAPSHOT", ignoreCase = true)

            val repoUrlKey = if (isSnapshot) "staging.repo.snapshot.url" else "staging.repo.release.url"
            val uri = URI(required(project, repoUrlKey))

            RepositoryConfig(
                name = "Staging",
                url = uri,
                credentials = RepositoryConfig.Credentials(
                    username = project.findProperty("staging.repo.username") as String?,
                    password = project.findProperty("staging.repo.password") as String?
                )
            )
        }

        PublishEnvironment.PRODUCTION -> {
            val version = project.version.toString()
            val isSnapshot = version.endsWith("-SNAPSHOT", ignoreCase = true)

            val repoUrlKey = if (isSnapshot) "prod.repo.snapshot.url" else "prod.repo.release.url"
            val uri = URI(required(project, repoUrlKey))

            RepositoryConfig(
                name = "Production",
                url = uri,
                credentials = RepositoryConfig.Credentials(
                    username = project.findProperty("prod.repo.username") as String?,
                    password = project.findProperty("prod.repo.password") as String?
                )
            )
        }
    }

    private fun required(project: Project, key: String): String =
        project.findProperty(key) as? String ?: error("Missing required Gradle property: $key")
}
