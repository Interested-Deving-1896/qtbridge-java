/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils

import org.gradle.api.Project
import org.qtproject.qt.bridge.resolver.QtResourceType
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
