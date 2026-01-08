/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

plugins {
    `kotlin-dsl`
    id("qtbridge.dev-embed")
    id("qtbridge.dev-publish")
}

group = "org.qtproject.qt.bridge"
version = "0.1.0"

val generateVersionFile by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/sources/version")
    val versionValue = providers.provider { version.toString() }
    val groupValue = providers.provider { group.toString() }
    val nameValue = providers.provider { name }
    val packageName = "org.qtproject.qtbridge"

    inputs.property("version", versionValue)
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
                const val VERSION = "${versionValue.get()}"
                const val GROUP = "${groupValue.get()}"
                const val NAME = "${nameValue.get()}"
            }
        """.trimIndent()
        )

        logger.lifecycle("Generated PluginVersion.kt with version: ${versionValue.get()}")
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
// TODO: QTBUG:142220
qtBridgePublishing {
    moduleName = "QtBridge Plugin"
    moduleDescription = """
    A Gradle plugin that configures and integrates the Qt Bridge environment for Kotlin or Java projects.
    This plugin ensures that all required Qt and native bridge resources are properly resolved, downloaded (if missing).
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
