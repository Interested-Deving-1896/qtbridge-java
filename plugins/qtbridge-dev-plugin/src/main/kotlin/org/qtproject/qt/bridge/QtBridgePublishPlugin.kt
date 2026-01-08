/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.qtproject.qt.bridge.publish.PublishContext
import org.qtproject.qt.bridge.publish.PublishEnvironment
import org.qtproject.qt.bridge.publish.QtBridgePublishingExtension
import org.qtproject.qt.bridge.publish.config.PublishConfigurer
import org.qtproject.qt.bridge.publish.config.PublishLoggerConfigurer
import org.qtproject.qt.bridge.publish.config.SigningConfigurer

class QtBridgePublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "qtBridgePublishing",
            QtBridgePublishingExtension::class.java,
            project
        )

        ensureRequiredPlugins(project)
        configureSources(project)

        project.afterEvaluate {
            val environment = PublishEnvironment.fromString(project.prop("publish.env"))
            val context = PublishContext(project, extension, environment)
            configureAll(context)
            configureJavadoc(project)
        }
    }

    private fun configureSources(project: Project) {
        project.extensions.findByType<JavaPluginExtension>()?.apply {
            withSourcesJar()
            withJavadocJar()
        }
    }

    private fun configureJavadoc(project: Project) {
        project.tasks.withType<Javadoc>().configureEach {
            val examplesDir = project.rootProject.projectDir.parentFile.resolve("examples")
            if (examplesDir.exists()) {
                (options as StandardJavadocDocletOptions).apply {
                    addStringOption("-snippet-path", examplesDir.absolutePath)
                }
            }
        }
    }

    private fun configureAll(context: PublishContext) {
        val configurers = buildList {
            add(PublishConfigurer())
            add(PublishLoggerConfigurer())
            if (context.environment == PublishEnvironment.PRODUCTION)
                add(SigningConfigurer())
        }
        configurers.forEach { it.configure(context) }
    }

    private fun ensureRequiredPlugins(project: Project) {
        val pluginManager = project.pluginManager
        if (!pluginManager.hasPlugin("java-gradle-plugin") &&
            !pluginManager.hasPlugin("java-library")) {
            pluginManager.apply("java-library")
        }
        pluginManager.apply("maven-publish")
        pluginManager.apply("signing")
    }
}

private fun Project.prop(key: String): String? {
    return findProperty(key)?.toString()
}
