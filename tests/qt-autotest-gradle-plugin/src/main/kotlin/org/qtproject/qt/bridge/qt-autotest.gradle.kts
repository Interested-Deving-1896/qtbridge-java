/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.internal.os.OperatingSystem
import org.gradle.api.tasks.JavaExec
import org.gradle.internal.jvm.Jvm
import org.gradle.kotlin.dsl.register
import java.io.File

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

// Configuration options for the plugin (settable in the tests' qtAutotest{} block)
abstract class QtAutotestExtension @Inject constructor(objects: ObjectFactory) {
    // Java main class to run (this MUST be set by the test project)
    val javaMain: Property<String> = objects.property(String::class.java)
    // Extra arguments for main()
    val args: ListProperty<String> = objects.listProperty(String::class.java)
    // Extra arguments for JVM
    val jvmArgs: ListProperty<String> = objects.listProperty(String::class.java)
}

// The plugin instance that becomes configurable in the test projects
val qtAutotest: QtAutotestExtension = extensions.create("qtAutotest", QtAutotestExtension::class.java)

// Task for running the test
val runQtTests = tasks.register<JavaExec>("runQtTests") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Run Qt Quick autotests (via Java main). Files named tst_*.qml in the autotest folder will be run"
    dependsOn(gradle.includedBuild("qmlbridge").task(":generateLibsAndCopy"))
    // Use repository root as working directory. This also causes problems with finding
    // the QML file because we can't set the workingDir to be the application dir
    workingDir = rootProject.projectDir
    // Set the main class to be executed (as set by the test project)
    mainClass.set(qtAutotest.javaMain)

    //check system property or environment variable
    val systemDir = System.getProperty("qtbridge.native.dir")
    val envDir = System.getenv("QTBRIDGE_NATIVE_DIR")
    if (!systemDir.isNullOrBlank()) {
        jvmArgs("-Dqtbridge.native.dir=$systemDir")
    }
    if (!envDir.isNullOrBlank()) {
        environment("QTBRIDGE_NATIVE_DIR", envDir)
    }

    if (OperatingSystem.current().isMacOsX) {
        // on macOS the JVM needs to be started on first thread
        jvmArgs("-XstartOnFirstThread")
        // Run JNI diagnostics on macOS (no need to run on every platform)
        jvmArgs("-Xcheck:jni")
        // Preload HotSpot signal chaining library to avoid handler clashes with Qt/QtTest,
        // as both check:jni and QtTest install their signal handlers.
        val javaHome = Jvm.current().javaHome
        val jsigPath = sequenceOf(file("$javaHome/lib/libjsig.dylib"),
                                  file("$javaHome/lib/server/libjsig.dylib"))
                                  .firstOrNull { it.exists() }
        if (jsigPath != null && jsigPath.exists())
            environment("DYLD_INSERT_LIBRARIES", jsigPath.absolutePath)
    }

    if (OperatingSystem.current().isWindows) {
        // On Windows, GUI Qt apps are not attached to a console by default.
        // This makes Qt attach to the Gradle/terminal console so that test
        // output (including qDebug/qWarning and QTest output) becomes visible.
        environment("QT_WIN_DEBUG_CONSOLE", "attach")
        environment("QT_ASSUME_STDERR_HAS_CONSOLE", "1")
    }

    // Append possible test project args (in qtAutotest{})
    jvmArgs(qtAutotest.jvmArgs.getOrElse(emptyList()))

    // Where to write the autotest results
    val resultsDir = layout.buildDirectory.dir("test-results/qtTest").get().asFile.apply { mkdirs() }
    // Arguments for the actual C++ quick test:
    // -input ; use test application project dir to scan for tst_*qml files
    // -o ; store results to file, as well as mirror on stdout
    val defaultArgs = listOf(
        "-input", project.projectDir.absolutePath,
        "-o", File(resultsDir, "TEST-${project.name}.xml").absolutePath + ",junitxml",
        "-o", "-,txt",
        "-maxwarnings", "0" // unlimited output
    )
    // Append possible test project args (set in qtAutotest{})
    args = defaultArgs + qtAutotest.args.getOrElse(emptyList())

    // Don't pop up a visible window when running a test
    environment("QT_QPA_PLATFORM" to "offscreen")
    val projectPathForLog = project.path

    doFirst {
        // Helper prints for troubleshooting
        println("runQtTests -> main=${mainClass.get()} in $projectPathForLog")
        println("  workingDir: $workingDir")
        println("  jvmArgs: $allJvmArgs")
        println("  args: $args")
        // Fail if qtAutotest{ javaMain } is not provided by the test project
        if (qtAutotest.javaMain.orNull.isNullOrBlank()) {
            throw GradleException(
                "qtAutotest.javaMain is not set. Example:\n" +
                        "  qtAutotest { javaMain.set(\"org.qt.autotest.TestApplication\") }"
            )
        }
    }
}

// These are callbacks that are called when Java/Kotlin plugins are
// applied (note that Kotlin implies Java)
pluginManager.withPlugin("java") {
    val sourceSets = extensions.getByType(SourceSetContainer::class.java)
    tasks.named<JavaExec>("runQtTests").configure {
        classpath = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).runtimeClasspath
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    // When Kotlin is applied, ensure that KSP is applied and annotations are processed
    pluginManager.apply("com.google.devtools.ksp")
    dependencies {
        add("ksp", "org.qtproject.qt.bridge:qmlbridge")
    }
}

// Core dependencies for every test application
dependencies {
    add("implementation", "org.qtproject.qt.bridge:qmlbridge")
    add("annotationProcessor", "org.qtproject.qt.bridge:qmlbridge")
}

// Run whenever 'test' (or 'check' or 'runQtTests' is run)
tasks.named(JavaPlugin.TEST_TASK_NAME).configure {
    dependsOn(runQtTests)
}
