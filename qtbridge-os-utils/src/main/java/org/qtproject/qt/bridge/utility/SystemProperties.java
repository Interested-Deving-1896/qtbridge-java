/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utility;

/**
 * Convenient access to commonly used system properties.
 */
final class SystemProperties {
    static String getOsName() {
        return System.getProperty("os.name");
    }

    static String getOsArch() {
        return System.getProperty("os.arch");
    }

    static String getJavaVersion() {
        return System.getProperty("java.version");
    }
}
