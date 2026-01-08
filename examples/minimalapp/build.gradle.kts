/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

plugins {
    id("org.qtproject.qt.bridge.qtbridge-plugin")
}

qtBridge {
    application {
        name = "MinimalApp"
        mainClass = "org.qtproject.qt.bridge.Main"
    }
}
