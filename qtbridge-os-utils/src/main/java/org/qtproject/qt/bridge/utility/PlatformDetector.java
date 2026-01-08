/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utility;

final class PlatformDetector {

    static OperatingSystem detectOS() {
        return detectOS(SystemProperties.getOsName());
    }

    static Architecture detectArch() {
        return detectArch(SystemProperties.getOsArch());
    }

    private static OperatingSystem detectOS(String osName) {
        if (osName == null) return OperatingSystem.UNKNOWN;
        String name = osName.toLowerCase();
        if (name.contains("mac") || name.contains("darwin")) {
            return OperatingSystem.MACOS;
        }
        if (name.contains("windows")) {
            return OperatingSystem.WINDOWS;
        }
        if (name.contains("nux") || name.contains("nix") ||
                name.contains("aix") || name.contains("sunos") ||
                name.contains("solaris")) {
            return OperatingSystem.LINUX;
        }
        return OperatingSystem.UNKNOWN;
    }

    private static Architecture detectArch(String osArch) {
        if (osArch == null) return Architecture.UNKNOWN;
        String arch = osArch.toLowerCase();
        if (arch.contains("amd64") || arch.contains("x86_64") || arch.contains("x64")){
            return Architecture.X86_64;

        }
        if (arch.contains("aarch64") || arch.contains("arm64")){
            return Architecture.AARCH64;

        }
        if (arch.contains("x86") || arch.contains("i386") ||
                arch.contains("i486") || arch.contains("i586") || arch.contains("i686")){
            return Architecture.X86;
        }
        if (arch.contains("arm")) {
            return Architecture.ARM;
        }
        return Architecture.UNKNOWN;
    }
}
