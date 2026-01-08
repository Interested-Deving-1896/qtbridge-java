/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;

@QMLRegistrable(name = "TestController", singleton = true)
public class TestSingleton {
    public void garbageCollect() {
        System.gc();
    }
}
