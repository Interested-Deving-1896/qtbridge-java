/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

/*
Builds a pre-compiled script plugin that autotests can use to set up
the autotest build. Autotests can include this plugin with:

plugins {
    id("org.qt.qt-autotest")
}

qtAutotest {
    // ... test specific configuration
}
*/

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
}
