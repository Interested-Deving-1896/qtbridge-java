/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.withType
import org.qtproject.qt.bridge.common.QtBridgeConstants
import org.qtproject.qt.bridge.utility.Platform

open class QtBridgeNativePluginExtension(project: Project) {
    val sourceDir: Property<String> = project.objects.property(String::class.java)
    val outputDir: Property<String> = project.objects.property(String::class.java)
        .convention("libs/${Platform.getLibraryDirectory()}")
}

class QtBridgeNativeBuildPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val extension = project.extensions.create(
            "QtBridgeNativeBuild",
            QtBridgeNativePluginExtension::class.java,
            project
        )

        val systemDir = System.getProperty(QtBridgeConstants.PROPERTY_NATIVE_DIR)
        val envDir = System.getenv(QtBridgeConstants.ENV_NATIVE_DIR)
        val skipNativeBuild = !systemDir.isNullOrBlank() || !envDir.isNullOrBlank()

        project.afterEvaluate {
            if (!systemDir.isNullOrBlank()) {
                project.tasks.withType<JavaExec>().configureEach {
                    jvmArgs("-D${QtBridgeConstants.PROPERTY_NATIVE_DIR}=$systemDir")
                }
            }
            if (!envDir.isNullOrBlank()) {
                project.tasks.withType<JavaExec>().configureEach {
                    environment(QtBridgeConstants.ENV_NATIVE_DIR, envDir)
                }
            }
        }

        if (skipNativeBuild) {
            val source = systemDir ?: envDir
            project.logger.lifecycle("QtBridgeNativeBuildPlugin: Skipping native build. Using external libs from $source")

            // Register placeholder tasks so dependencies still resolve
            val platform = Platform.getLibraryDirectory()
            val platformCap = platform.replaceFirstChar { it.uppercaseChar() }

            val skippedTaskNames = listOf(
                "configureCMake$platformCap",
                "buildCMake$platformCap",
                "moveLib",
                "generateLibs",
                "generateLibsAndCopy",
                "generateJarFile"
            )

            skippedTaskNames.forEach { name ->
                project.tasks.register(name) {
                    group = "QtBridge"
                    description =
                        "Skipped because ${QtBridgeConstants.PROPERTY_NATIVE_DIR} or ${QtBridgeConstants.ENV_NATIVE_DIR} is set."
                    doFirst {
                        project.logger.lifecycle("Skipping task $name due to external native libs.")
                    }
                }
            }
            return
        }

        val osPrefix = Platform.getLibraryDirectory()
        val sharedLibSuffix = Platform.getSharedLibrarySuffix()
        val cmakeBuildDir = project.layout.buildDirectory.dir("cmake")

        val cmakeBuildTask = registerCMakeBuildTasks(project, osPrefix, cmakeBuildDir, extension)
        val moveTask = registerMoveLibTask(project, osPrefix, sharedLibSuffix, cmakeBuildDir, extension)
        registerGenerateLibsTasks(project, cmakeBuildTask, moveTask)
        registerJarTask(project, osPrefix, sharedLibSuffix, cmakeBuildDir, extension.outputDir.get(), cmakeBuildTask)
    }

    private fun registerCMakeBuildTasks(
        project: Project,
        platform: String,
        cmakeBuildDir: Provider<Directory>,
        extension: QtBridgeNativePluginExtension
    ): TaskProvider<Exec> {
        val cmakeCommand = if (Platform.isWindows()) "cmake.exe" else "cmake"
        val cmakePlatformBuildDir = cmakeBuildDir.map { it.dir(platform) }
        val platformCapitalized = platform.replaceFirstChar { it.uppercaseChar() }

        val configure = project.tasks.register("configureCMake$platformCapitalized", Exec::class.java) {
            commandLine(
                cmakeCommand,
                "-S", project.file(extension.sourceDir.get()),
                "-B", cmakePlatformBuildDir.get().asFile,
                "-DCMAKE_BUILD_TYPE=Release",
            )
        }

        return project.tasks.register("buildCMake$platformCapitalized", Exec::class.java) {
            dependsOn(configure)
            workingDir(cmakePlatformBuildDir.get().asFile)
            commandLine(cmakeCommand, "--build", ".")
        }
    }

    private fun registerMoveLibTask(
        project: Project,
        platform: String,
        sharedLibSuffix: String,
        cmakeBuildDir: Provider<Directory>,
        extension: QtBridgeNativePluginExtension
    ): TaskProvider<Copy> {
        return project.tasks.register("moveLib", Copy::class.java) {
            val platformBuildDir = cmakeBuildDir.map { it.dir(platform) }

            from(platformBuildDir) {
                include("*$sharedLibSuffix")
            }

            // on Windows, check Debug subdirectory
            if (Platform.isWindows()) {
                from(platformBuildDir.map { it.dir("Debug") }) {
                    include("*$sharedLibSuffix")
                }
            }

            into(extension.outputDir.get())

            doFirst {
                val buildDir = platformBuildDir.get().asFile
                val libs = buildDir.listFiles { file ->
                    file.name.endsWith(sharedLibSuffix)
                }?.toList() ?: emptyList()

                if (libs.isEmpty() && Platform.isWindows()) {
                    val debugDir = buildDir.resolve("Debug")
                    val debugLibs = debugDir.listFiles { file ->
                        file.name.endsWith(sharedLibSuffix)
                    }?.toList() ?: emptyList()

                    when {
                        debugLibs.isNotEmpty() -> {
                            project.logger.lifecycle("Found libraries in Debug folder: ${debugLibs.map { it.name }}")
                        }
                        else -> {
                            project.logger.warn("No libraries found in build or Debug folders")
                        }
                    }
                } else if (libs.isNotEmpty()) {
                    project.logger.lifecycle("Found libraries in build folder: ${libs.map { it.name }}")
                }
            }
        }
    }

    private fun registerGenerateLibsTasks(
        project: Project,
        cmakeBuild: TaskProvider<Exec>,
        moveLib: TaskProvider<Copy>
    ) {
        project.tasks.register("generateLibs") {
            dependsOn(cmakeBuild)
        }

        project.tasks.register("generateLibsAndCopy") {
            dependsOn("generateLibs")
            dependsOn(moveLib)
        }
    }

    private fun registerJarTask(
        project: Project,
        platform: String,
        sharedLibSuffix: String,
        cmakeBuildDir: Provider<Directory>,
        outputDir: String,
        cmakeBuild: TaskProvider<Exec>
    ) {
        project.tasks.register("generateJarFile", Jar::class.java) {
            dependsOn(cmakeBuild)
            val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)
            val mainOutput = javaExtension.sourceSets.getByName("main").output
            from(mainOutput)
            from(cmakeBuildDir.map { it.dir(platform) }) {
                include("*$sharedLibSuffix")
                into(outputDir)
            }
            archiveClassifier.set("with-native-libs")
        }
    }
}
