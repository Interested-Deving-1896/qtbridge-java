/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

plugins {
    id("org.qtproject.qt.bridge.qt-autotest")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.+")
}

qtAutotest {
    javaMain.set("org.qtproject.qt.bridge.autotest.TestApplication")
}
