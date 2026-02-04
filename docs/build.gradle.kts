/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.api.tasks.PathSensitivity
import java.io.File

plugins {
    java // For Javadoc task type
}

java {
    // Ensure {@snippet} (available since Java 18)
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}
// Output directories for documentation
// Entry-point overview documentation
val moduleDocOutputDir: Provider<Directory> = layout.buildDirectory.dir("module")
// Javadoc API documentation
val apiDocOutputDir: Provider<Directory> = moduleDocOutputDir.map { it.dir("api") }
val snippetRoot: File = rootProject.file("examples")

// Add a resolvable configuration
repositories {
    mavenCentral()
    google()
}
val qmlbridgeClassPath = configurations.create("qmlbridgeConfig")
dependencies {
    qmlbridgeClassPath("org.qtproject.qt.bridge:qmlbridge")
}
val copyHtmlDocs by tasks.registering(Copy::class) {
    from(project.file("qtbridge-gradle-plugin.html"))
    into(apiDocOutputDir)
}
// Javadoc API + Annotations + overview documentation
val javadocPublicApi by tasks.registering(Javadoc::class) {
    // Public classes that we want to generate the documentation for
    val apiFiles = listOf(
        "org/qtproject/qt/bridge/core/QtListModel.java",
        "org/qtproject/qt/bridge/core/QtProperty.java",
        "org/qtproject/qt/bridge/core/QtListModelObserver.java",
        "org/qtproject/qt/bridge/core/QtPropertyObserver.java",
        "org/qtproject/qt/bridge/core/QtAbstractApplication.java",
        "org/qtproject/qt/bridge/core/QtQuickApplication.java",
        "org/qtproject/qt/bridge/core/QtQmlChildren.java"
    ).map { project.file("../qmlbridge/src/main/java/$it") }

    val annotationsFiles = listOf(
        "org/qtproject/qt/bridge/annotations/QMLRegistrable.java",
        "org/qtproject/qt/bridge/annotations/QMLSignals.java",
        "org/qtproject/qt/bridge/annotations/QMLComplete.java",
        "org/qtproject/qt/bridge/annotations/QMLIgnore.java",
    ).map { project.file("../qmlbridge/src/main/java/$it") }

    // Add OverviewSnippets from the snippet example so that it can be
    // referred to from overview.html
    val overviewSnippets = snippetRoot.resolve("snippets/src/main/java/org/qtproject/qt/bridge/OverviewSnippets.java")

    // Tell Gradle what non-source files affect the output so that when we
    // edit them, Gradle docs:all target will detect changes without needing
    // to call 'clean'
    inputs.file(project.file("overview.html"))
        .withPropertyName("overviewHtml")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(snippetRoot)
        .withPropertyName("snippetSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(project.file("qtbridge-gradle-plugin.html"))
        .withPropertyName("gradlePluginHtml")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    // Which files to process
    source = project.files(apiFiles + annotationsFiles + overviewSnippets).asFileTree
    // Use the configuration that has access to repositories
    classpath = qmlbridgeClassPath
    // Where to put the output
    destinationDir = apiDocOutputDir.get().asFile

    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        charSet = "UTF-8"
        // Module overview content
        overview = project.file("overview.html").absolutePath
        // Code snippets from examples
        addStringOption("-snippet-path", snippetRoot.absolutePath)
        docTitle = "Overview"
        addBooleanOption("Xdoclint:all", true)
        addBooleanOption("Werror", true)  // make warnings fail the build task
        // todo: add links
        links("https://docs.oracle.com/en/java/javase/21/docs/api/")
        // Optional titles
        windowTitle = "Qt Java Bridge"
        header = "Qt Java Bridge"
    }
    finalizedBy(copyHtmlDocs)
}

// Entry-point task that builds the documentation
tasks.register("all") {
    dependsOn(javadocPublicApi)
    doLast {
        println()
        println("Useful links")
        println("Documentation root is at: ${moduleDocOutputDir.get().asFile}")
        println("Landing page: ${apiDocOutputDir.get().file("index.html").asFile}")
        println("List of all classes: ${apiDocOutputDir.get().file("allclasses-index.html").asFile}")
        println("List of all annotations: ${apiDocOutputDir.get().file("org/qtproject/qt/bridge/annotations/package-summary.html").asFile}")
    }
}

// Make 'check' depend on docs so that builds fail if there are docs errors
tasks.named("check") {
    dependsOn(javadocPublicApi)
}
