/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.qtproject.qt.bridge.resolver.factory.QtResourceType
import java.io.File

internal fun QtResourceType.getDestinationDir(project: Project): String =
    project.gradle.gradleUserHomeDir
        .resolve("caches/qt-downloads/$relativeDestination")
        .absolutePath

internal fun File.isArchive(): Boolean {
    val lowerName = name.lowercase()
    return lowerName.endsWith(".zip") ||
            lowerName.endsWith(".tar.gz") ||
            lowerName.endsWith(".tgz") ||
            lowerName.endsWith(".tar")
}

internal fun Project.prop(key: String): String? {
    return findProperty(key)?.toString()
}
internal fun String.isMocJson(): Boolean = endsWith("_moc.json")
internal fun String.toQmlModuleUri(): String = removeSuffix("_moc.json")
internal fun String.toQmlModuleDir(importRoot: File): File = importRoot.resolve(replace('.', File.separatorChar))

internal fun Project.appQmlImportDir(
    sourceSetName: String = SourceSet.MAIN_SOURCE_SET_NAME
): File = layout.buildDirectory.dir("qtbridge/qml-imports/$sourceSetName").get().asFile

internal fun Project.mocJsonDir(
    sourceSetName: String = SourceSet.MAIN_SOURCE_SET_NAME
): File = layout.buildDirectory.dir("generated/ksp/$sourceSetName/resources").get().asFile

internal fun Project.qmllsIniFile(): File = layout.buildDirectory.file(".qt/.qmlls.build.ini").get().asFile

internal fun Project.mocFiles():List<File> {
    return fileTree(mocJsonDir()) { include("**/*_moc.json") }.files.filter { it.name.isMocJson() }
}

internal fun File.toqmllsSectionName(): String {
    // .qmlls.build.ini uses <SLASH> escaping for '/'
    val p = absolutePath.replace('\\', '/')
    return p.split('/').joinToString(separator = "") { part ->
        if (part.isEmpty()) "" else "<SLASH>$part"
    }
}

internal fun File.isValidQtRoot(): Boolean {
    if (!exists())
        return false
    if (!isDirectory)
        return false

    val binDir = resolve("bin")
    if (!binDir.isDirectory)
        return false

    val libDir = resolve("lib")
    val libExecDir = resolve("libExec")
    if (!libDir.isDirectory && !libExecDir.isDirectory)
        return false

    if (!hasQtInstallationMarkers())
        return false

    return true
}

internal fun File.hasQtInstallationMarkers(): Boolean {
    val binDir = resolve("bin")
    if (!listOf("qmake", "qmake6")
        .any { tool -> binDir.resolve(tool).exists() || binDir.resolve("$tool.exe").exists() })
        return false;

    if (!listOf("qmllint", "qmlls")
        .any() { tool -> binDir.resolve(tool).exists() || binDir.resolve("$tool.exe").exists() })
        return false;

    return true
}
