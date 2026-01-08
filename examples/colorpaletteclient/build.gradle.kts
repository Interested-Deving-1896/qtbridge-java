/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

plugins {
    id("org.qtproject.qt.bridge.qtbridge-plugin")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.+")
}

qtBridge {
    application {
        name = "colorpaletteclient"
        mainClass = "org.qtproject.qt.bridge.MainKt"
    }
}
