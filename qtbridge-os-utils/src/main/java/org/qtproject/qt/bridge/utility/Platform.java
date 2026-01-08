/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utility;

public final class Platform {
    private static OperatingSystem os;
    private static Architecture arch;

    private static OperatingSystem getOS() {
        if (os == null) {
            os = PlatformDetector.detectOS();
        }
        return os;
    }

    private static Architecture getArch() {
        if (arch == null) {
            arch = PlatformDetector.detectArch();
        }
        return arch;
    }

    public static boolean isWindows() {
        return getOS() == OperatingSystem.WINDOWS;
    }

    public static boolean isMacOS() {
        return getOS() == OperatingSystem.MACOS;
    }

    public static boolean isLinux() {
        return getOS() == OperatingSystem.LINUX;
    }

    public static boolean isUnix() {
        return getOS() == OperatingSystem.LINUX || getOS() == OperatingSystem.MACOS;
    }

    public static boolean isX86_64() {
        return getArch() == Architecture.X86_64;
    }

    public static boolean isAarch64() {
        return getArch() == Architecture.AARCH64;
    }

    public static boolean isX86() {
        return getArch() == Architecture.X86;
    }

    public static boolean is64Bit() {
        return getArch().is64Bit();
    }

    public static boolean is32Bit() {
        return getArch().is32Bit();
    }

    public static boolean isSupported() {
        return getOS().isKnown() && getArch().isKnown();
    }

    public static void requireSupported() {
        if (!isSupported())
            throw new UnsupportedPlatformException("Unsupported platform: " + getDescription());
    }

    public static String getLibraryDirectory() {
        requireSupported();
        String osPart = getOS().getDirectoryName();
        Architecture arch = getArch();
        String archPart = switch (arch) {
            case X86_64 -> (getOS() == OperatingSystem.WINDOWS) ? "amd64" : "x86_64";
            case AARCH64 -> (getOS() == OperatingSystem.LINUX) ? "aarch64" : "arm64";
            case X86 -> "x86";
            case ARM -> "arm";
            default -> "unknown";
        };
        return osPart + "_" + archPart;
    }

    public static String getSharedLibrarySuffix() {
        return "." + getSharedLibraryExtension();
    }

    public static String getSharedLibraryExtension() {
        requireSupported();
        if (isWindows())
            return "dll";
        if (isMacOS())
            return "dylib";
        return "so";
    }

    public static String getDescription() {
        return getOS().getDisplayName() + " " + getArch().getDisplayName();
    }

    public static String getDetailedInfo() {
        return String.format(
                "Platform: %s%n" +
                        "  OS: %s (detected from: %s)%n" +
                        "  Architecture: %s (detected from: %s)%n" +
                        "  Java Version: %s%n",
                getDescription(),
                getOS().getDisplayName(), SystemProperties.getOsName(),
                getArch().getDisplayName(), SystemProperties.getOsArch(),
                SystemProperties.getJavaVersion()
        );
    }

    // this is intentionally not public to be able to use in testing
    static void reset() {
        os = null;
        arch = null;
    }
}
