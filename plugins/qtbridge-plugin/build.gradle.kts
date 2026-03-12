/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

import java.util.Properties

plugins {
    `kotlin-dsl`
    id("qtbridge.dev-embed")
    id("qtbridge.dev-publish")
}

fun loadQtBridgeVersions(): Properties {
    val versionsFile = generateSequence(rootDir) { it.parentFile }
        .map { it.resolve("qtbridge-versions.properties") }
        .firstOrNull { it.isFile }
        ?: error("Unable to locate qtbridge-versions.properties from ${rootDir.absolutePath}")

    return Properties().apply {
        versionsFile.inputStream().use { input -> load(input) }
    }
}

// Returns version string for 'key' entry in qtbridge-versions.properties
fun qtBridgeVersion(key: String): String {
    return loadQtBridgeVersions().getProperty(key)
        ?: error("Missing '$key' in qtbridge-versions.properties")
}

group = "org.qtproject.qt.bridge"
version = qtBridgeVersion("qt.bridge.application.plugin.version")

val generateVersionFile by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/sources/version")
    val qtLibrariesVersionValue = providers.provider { qtBridgeVersion("qt.libraries.version") }
    val applicationPluginVersionValue = providers.provider { qtBridgeVersion("qt.bridge.application.plugin.version") }
    val bridgeNativeVersionValue = providers.provider { qtBridgeVersion("qt.bridge.native.version") }
    val bridgeJvmVersionValue = providers.provider { qtBridgeVersion("qt.bridge.jvm.version") }
    val groupValue = providers.provider { project.group }
    val nameValue = providers.provider { project.name }
    val packageName = "org.qtproject.qtbridge"

    inputs.property("qtLibrariesVersion", qtLibrariesVersionValue)
    inputs.property("applicationPluginVersion", applicationPluginVersionValue)
    inputs.property("bridgeNativeVersion", bridgeNativeVersionValue)
    inputs.property("bridgeJvmVersion", bridgeJvmVersionValue)
    inputs.property("group", groupValue)
    inputs.property("name", nameValue)
    inputs.property("packageName", packageName)
    outputs.dir(outputDir)

    doLast {
        val packagePath = packageName.replace('.', '/')
        val versionFile = outputDir.get().asFile.resolve("$packagePath/PluginVersion.kt")

        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
            /*
             * Copyright (C) 2025 The Qt Company Ltd.
             * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
             */
            package $packageName

            /**
             * Auto-generated file containing plugin version information.
             * Do not edit manually.
             */
            internal object PluginVersion {
                const val QT_LIBRARIES_VERSION = "${qtLibrariesVersionValue.get()}"
                const val QT_BRIDGE_APPLICATION_PLUGIN_VERSION = "${applicationPluginVersionValue.get()}"
                const val QT_BRIDGE_NATIVE_VERSION = "${bridgeNativeVersionValue.get()}"
                const val QT_BRIDGE_JVM_VERSION = "${bridgeJvmVersionValue.get()}"
                const val GROUP = "${groupValue.get()}"
                const val NAME = "${nameValue.get()}"
            }

        """.trimIndent()
        )

        logger.lifecycle(
            "Generated PluginVersion.kt with\n" +
            "    Application plugin version: ${applicationPluginVersionValue.get()}\n" +
            "    Qt libraries version: ${qtLibrariesVersionValue.get()}\n" +
            "    Qt Bridge native lib version: ${bridgeNativeVersionValue.get()}\n" +
            "    Qt Bridge JVM lib version: ${bridgeJvmVersionValue.get()}")
    }
}

sourceSets {
    main {
        java.srcDir(generateVersionFile)
    }
}

tasks.named("compileKotlin") {
    dependsOn(generateVersionFile)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

qtBridgePublishing {
    moduleName = "Qt Bridge for JVM - Gradle Plugin"
    moduleDescription = """
        Gradle plugin for configuring and integrating the Qt Bridge environment in Kotlin and Java projects.
        It automatically resolves, downloads (when necessary), and wires all required Qt runtime and native bridge components.
    """.trimIndent()
}

gradlePlugin {
    plugins {
        create("qtbridgePlugin") {
            id = "org.qtproject.qt.bridge.qtbridge-plugin"
            implementationClass = "org.qtproject.qt.bridge.QtBridgePlugin"
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.1.20-1.0.32")
    embed("org.qtproject.qt.bridge:qtbridge-os-utils")
}
