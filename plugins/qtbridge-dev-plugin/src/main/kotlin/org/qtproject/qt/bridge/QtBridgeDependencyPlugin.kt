/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*

class QtBridgeDependencyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("java-library")

        val embed = project.configurations.create("embed") {
            isCanBeConsumed = false
            isCanBeResolved = true
            isTransitive = true
        }
        project.extensions.configure<JavaPluginExtension> {
            sourceSets.named("main") {
                compileClasspath += embed
                runtimeClasspath += embed
            }
            sourceSets.named("test") {
                compileClasspath += embed
                runtimeClasspath += embed
            }
        }

        // Bundle into jar
        project.tasks.named<Jar>("jar") {
            from({
                embed.map { file ->
                    if (file.isDirectory) file else project.zipTree(file)
                }
            }) {
                exclude("META-INF/*.SF")
                exclude("META-INF/*.DSA")
                exclude("META-INF/*.RSA")
            }

            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}
