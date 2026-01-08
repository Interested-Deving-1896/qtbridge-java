/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utility;

enum OperatingSystem {
    WINDOWS("Windows", "win"),
    MACOS("macOS", "macos"),
    LINUX("Linux", "linux"),
    UNKNOWN("Unknown", "unknown");

    private final String displayName;
    private final String directoryName;

    OperatingSystem(String displayName, String directoryName) {
        this.displayName = displayName;
        this.directoryName = directoryName;
    }

    String getDisplayName() {
        return displayName;
    }

    String getDirectoryName() {
        return directoryName;
    }

    boolean isKnown() {
        return this != UNKNOWN;
    }
}
