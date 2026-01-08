/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.publish.config

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.qtproject.qt.bridge.publish.PublishContext
import org.qtproject.qt.bridge.publish.PublishEnvironment
import org.qtproject.qt.bridge.publish.QtBridgePublishingExtension

internal class PublishConfigurer : Configurer {

    override fun configure(context: PublishContext) {
        val project = context.project
        val extension = context.extension
        val environment = context.environment

        project.extensions.configure<PublishingExtension> {
            configurePublications(project, extension)
            configureRepositories(project, environment)
        }
    }

    private fun PublishingExtension.configurePublications(project: Project, extension: QtBridgePublishingExtension) {
        val hasJavaComponent = project.components.findByName("java") != null
        val hasPluginPublishPublication = project.plugins.hasPlugin("java-gradle-plugin")
        publications {
            when {
                hasPluginPublishPublication -> configurePluginPublication(project, extension)
                hasJavaComponent -> {
                    create<MavenPublication>("maven") {
                        from(project.components["java"])
                        artifactId = extension.artifactId.get()
                        configurePom(extension)
                    }
                }
                else -> project.logger.warn("No 'java' component or 'java-gradle-plugin' detected. No publications configured by QtBridgePublishPlugin.")
            }
        }
    }

    private fun PublishingExtension.configurePluginPublication(
        project: Project, extension: QtBridgePublishingExtension
    ) {
        project.afterEvaluate {
            publications.named<MavenPublication>("pluginMaven") {
                artifactId = extension.artifactId.get()
                configurePom(extension)
            }
        }
    }

    private fun MavenPublication.configurePom(extension: QtBridgePublishingExtension) {
        pom {
            name.set(extension.moduleName)
            description.set(extension.moduleDescription)
            url.set(extension.repositoryUrl)

            licenses {
                extension.licenses.forEach {
                    license {
                        name.set(it.licenseName)
                        url.set(it.licenseUrl)
                    }
                }
            }

            developers {
                developer {
                    organization.set(extension.organization)
                    url.set(extension.developerUrl)
                    email.set(extension.developerEmail)
                }
            }

            scm {
                val repo = extension.repositoryUrl.get()
                val gitUrl = extension.gitUrl.get()
                val codeReviewUrl = extension.codeReviewUrl.get()
                url.set(repo)
                connection.set("scm:git:$gitUrl")
                developerConnection.set("scm:git:$codeReviewUrl")
            }
        }
    }

    private fun PublishingExtension.configureRepositories(project: Project, environment: PublishEnvironment) {
        repositories {
            if (environment == PublishEnvironment.LOCAL) {
                mavenLocal()
                return@repositories
            }
            val config = RepositoryConfigFactory.from(project, environment)
            maven {
                name = config.name
                url = config.url
                config.credentials?.let { creds ->
                    credentials {
                        username = creds.username
                        password = creds.password
                    }
                }
            }
        }
    }
}
