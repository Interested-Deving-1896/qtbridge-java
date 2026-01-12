/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

plugins {
    id("java-library")
    kotlin("jvm") version "2.1.20"
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" apply false
    id("qtbridge.dev-build-native")
    id("qtbridge.dev-embed")
    id("qtbridge.dev-publish")
}

group = "org.qtproject.qt.bridge"
version = "0.1"

java {
}

repositories {
    mavenCentral()
}

QtBridgeNativeBuild {
    sourceDir = "src/main/cpp"
}

qtBridgePublishing {
    moduleName = "QtBridge QML"
    moduleDescription = """
        Bridge library between Java/Kotlin and Qt QML. Provides a bidirectional binding layer that enables
        seamless interaction between the native UI layer and Java/Kotlin code. Supports exposing objects,
        properties, and methods, as well as handling callbacks and some data models. Read documentation on //LINK
    """.trimIndent()
}


dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.20-1.0.32")
    embed("org.qtproject.qt.bridge:qtbridge-os-utils")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation(kotlin("test"))
    implementation("org.json:json:20251224")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
