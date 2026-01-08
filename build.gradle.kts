/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

repositories {
    mavenCentral()
}

plugins {
    // Define common versions in root project so that subprojects always use
    // the same versions
    id("org.jetbrains.kotlin.jvm") version "2.1.20" apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" apply false
}
