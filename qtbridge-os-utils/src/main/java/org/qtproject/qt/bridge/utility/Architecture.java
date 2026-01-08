/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utility;

enum Architecture {
    X86_64("x86_64", true),
    AARCH64("ARM64", true),
    X86("x86", false),
    ARM("ARM", false),
    UNKNOWN("Unknown", false);

    private final String displayName;
    private final boolean is64Bit;

    Architecture(String displayName, boolean is64Bit) {
        this.displayName = displayName;
        this.is64Bit = is64Bit;
    }

    String getDisplayName() {
        return displayName;
    }


    boolean is64Bit() {
        return is64Bit;
    }

    boolean is32Bit() {
        return !is64Bit && this != UNKNOWN;
    }

    boolean isKnown() {
        return this != UNKNOWN;
    }
}
