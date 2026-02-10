/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge

import java.io.IOException;
import org.qtproject.qt.bridge.core.QtQuickApplication
import org.qtproject.qt.bridge.ColorServer

fun main(args: Array<String>) {
    // Create and start the bundled Java-server
    val colorServer = try {
        ColorServer().also{ server -> server.start() }
    } catch (e: IOException) {
        System.err.println("Unable to start the bundled Java server")
        null
    }
    // Execute the QML application
    try {
        QtQuickApplication(args).execute()
    } finally {
        colorServer?.stop()
    }
}
