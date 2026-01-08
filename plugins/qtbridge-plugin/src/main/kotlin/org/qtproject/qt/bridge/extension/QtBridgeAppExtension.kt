/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.extension

import org.gradle.api.Project

open class QtBridgeAppExtension(project: Project) {
    val name = project.objects.property(String::class.java)
    val mainClass = project.objects.property(String::class.java)
    val jvmArgs = project.objects.listProperty(String::class.java).convention(emptyList<String>())
}
