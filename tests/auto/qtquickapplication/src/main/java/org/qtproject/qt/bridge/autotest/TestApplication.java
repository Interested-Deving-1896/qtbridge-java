/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.core.QtQuickApplication;

public class TestApplication {

    private static Object[][] initCases() {
        return new Object[][]{
            // Broken QML
            { "import QtQuick broken QML\n", false },
            // Fine
            { "import QtQuick\nItem { width: 100; height: 100 }\n", true },
            // Already loaded
            { "import QtQuick\nItem { width: 100; height: 100 }\n", false }
        };
    }

    public static void main(String[] args) {
        QtQuickApplication app = new QtQuickApplication(
            new String[]{"--qtbridge-suppress-qml-warnings"});

        for (Object[] row : initCases()) {
            String qml = (String) row[0];
            boolean expectedSuccess = (boolean) row[1];
            try {
                app.loadQmlContent(qml);
                if (!expectedSuccess) {
                    System.out.println("Expected load to fail, but it succeeded: " + qml);
                    System.exit(1);
                }
            } catch (Throwable t) {
                if (expectedSuccess) {
                    System.out.println("Expected load to succeed, but it failed: " + qml);
                    t.printStackTrace();
                    System.exit(1);
                }
            }
        }
        System.exit(0);
    }
}
