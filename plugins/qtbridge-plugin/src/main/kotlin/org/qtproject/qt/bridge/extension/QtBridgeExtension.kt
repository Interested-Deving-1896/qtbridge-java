/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.extension

import org.gradle.api.Project

open class QtBridgeExtension(project: Project) {
    val qtLibraryPath = project.objects.property(String::class.java)
    val qtBridgeLibraryPath = project.objects.property(String::class.java)
}
