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
        app.execute(args);
    }
}
