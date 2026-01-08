/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.qtproject.qt.bridge:qtbridge-os-utils")
}

gradlePlugin {
    plugins {
        create("qtbridgeNativeLib"){
            id = "qtbridge.dev-build-native"
            implementationClass = "org.qtproject.qt.bridge.QtBridgeNativeBuildPlugin"
        }
    }
}

gradlePlugin {
    plugins {
        create("qtbridgeEmbed"){
            id = "qtbridge.dev-embed"
            implementationClass = "org.qtproject.qt.bridge.QtBridgeDependencyPlugin"
        }
    }
}

gradlePlugin {
    plugins {
        create("qtbridgePublish"){
            id = "qtbridge.dev-publish"
            implementationClass = "org.qtproject.qt.bridge.QtBridgePublishPlugin"
        }
    }
}
