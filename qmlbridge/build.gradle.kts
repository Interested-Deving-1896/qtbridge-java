/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

import java.util.Properties

plugins {
    id("java-library")
    kotlin("jvm") version "2.1.20"
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" apply false
    id("qtbridge.dev-build-native")
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
version = qtBridgeVersion("qt.bridge.jvm.version")

java {
}

repositories {
    mavenCentral()
}

QtBridgeNativeBuild {
    sourceDir = "src/main/cpp"
}

qtBridgePublishing {
    moduleName = "Qt Bridge for JVM"
    moduleDescription = """
        Bridge library enabling bidirectional communication between Java/Kotlin and Qt QML.
        Provides a binding layer for exposing objects, properties and methods, supporting callbacks and selected
        data models to enable seamless interaction between the native UI layer and JVM-based application logic.
    """.trimIndent()
}


dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.20-1.0.32")
    embed("org.qtproject.qt.bridge:qtbridge-os-utils")
    implementation("org.json:json:20251224")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
