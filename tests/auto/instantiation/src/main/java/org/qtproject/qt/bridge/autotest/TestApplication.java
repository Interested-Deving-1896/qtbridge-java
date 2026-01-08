/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.utils.QtAutotestApplication;

public class TestApplication
{
    public static void main(String[] args) {
        final QtAutotestApplication app = new QtAutotestApplication();
        // Garbage collect to catch any GC related crashes (JNI issues in particular).
        // The 'unreachable' instance below is not kept alive by the QtQuickApplication,
        // so it should be garbage collected and native resources freed
        Singleton1 unreachable = new Singleton1();
        unreachable = null;
        System.gc();
        app.execute(args);
    }
}
