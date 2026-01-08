/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

rootProject.name = "QtBridgeForJava"

include("docs")
// Build projects as composite builds to be able to reuse them in a plugins
includeBuild("qtbridge-os-utils"){
    dependencySubstitution {
        substitute(module("org.qtproject.qt.bridge:qtbridge-os-utils"))
            .using(project(":"))
    }
}
includeBuild("qmlbridge") {
    dependencySubstitution {
        substitute(module("org.qtproject.qt.bridge:qmlbridge"))
            .using(project(":"))
    }
}

// Find and include all autotest directories (if you need to omit directories, do that here)
file("tests/auto").listFiles{ f -> f.isDirectory() }?.forEach { dir ->
    val path = ":tests:auto:${dir.name}"
    // Skip benchmark directory by default unless benchmark property is set
    // ./gradlew check -Pbenchmark to include benchmark tests
    val includeBenchmarks = gradle.startParameter.projectProperties.containsKey("benchmark")
    val isBenchmarkProject = dir.name.contains("benchmark", ignoreCase = true)
    if (!isBenchmarkProject || includeBenchmarks) {
        include(path)
        project(path).projectDir = dir
    }
}
// Build gradle-plugin that helps set up the individual autotests
includeBuild("tests/qt-autotest-gradle-plugin")

// Find and include all example directories (if you need to omit directories, do that here)
file("examples").listFiles { f -> f.isDirectory()
        && f.name != "qt-example-gradle-plugin"
        && f.name != "starter" }?.forEach { dir ->
    val path = ":examples:${dir.name}"
    include(path)
    project(path).projectDir = dir
}
// Include local build for development plugins
// `qtbridge-dev-plugin` may contain multiple helper plugins that assist
// during development and later with publishing.
includeBuild("plugins/qtbridge-dev-plugin")
includeBuild("plugins/qtbridge-plugin")
