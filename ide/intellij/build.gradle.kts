/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "org.qt"
version = "0.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3")
        plugin("com.redhat.devtools.lsp4ij:0.19.1")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
        }
        changeNotes = "Initial version"
    }
}

//resolves lsp4ij zip from the Gradle dependency cache
val lsp4ijArtifact: Provider<File> = provider {
    configurations.getByName("intellijPlatformPluginDependency")
        .resolvedConfiguration
        .resolvedArtifacts
        .first { it.moduleVersion.id.module.name == "com.redhat.devtools.lsp4ij" }
        .file
}

// todo: needs to be tested on different OS to ensure the installation is good to go
fun intellijPluginsDir(): File {
    val os = System.getProperty("os.name").lowercase()
    val base = when {
        os.contains("win") -> File(System.getenv("APPDATA"), "JetBrains")
        os.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/JetBrains")
        else -> File(System.getProperty("user.home"), ".config/JetBrains")
    }
    return base.listFiles()
        ?.filter { it.name.startsWith("IntelliJIdea") }
        ?.maxByOrNull { it.name }
        ?.resolve("plugins")
        ?: error("IntelliJ IDEA plugins directory not found under $base")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    // Bundles both plugin zips into build/distributions/
    register<Copy>("buildPluginPack") {
        dependsOn("buildPlugin")
        into(layout.buildDirectory.dir("distributions"))
        from(lsp4ijArtifact)
    }

    //install both plugins into the local IntelliJ IDEA
    register("installPlugin") {
        dependsOn("buildPlugin")
        doLast {
            val pluginsDir = intellijPluginsDir()
            logger.lifecycle("Installing plugins into: $pluginsDir")
            listOf(
                named("buildPlugin").get().outputs.files.singleFile,
                lsp4ijArtifact.get()
            ).forEach { zip ->
                copy { from(zipTree(zip)); into(pluginsDir) }
                logger.lifecycle("Installed: ${zip.name}")
            }
            logger.lifecycle("Restart IntelliJ IDEA to apply.")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
