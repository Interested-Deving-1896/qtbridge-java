/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge

import org.qtproject.qt.bridge.core.QtQuickApplication

fun main(args: Array<String>) {
    QtQuickApplication(args).apply {
        execute()
    }
}
