/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

plugins {
    id("org.qtproject.qt.bridge.qt-autotest")
}

qtAutotest {
    javaMain.set("org.qtproject.qt.bridge.autotest.QmlIgnoreTestApplication")
}
