/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge;

import org.qtproject.qt.bridge.core.QtQuickApplication;

// @start region="qtquickapplication-main-java"
public class Main {
    public static void main(String[] args) {
        final QtQuickApplication app = new QtQuickApplication(args);
        app.execute();
    }
}
// @end
