/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utility;

import org.junit.jupiter.api.Test;


import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformDetectorTest {

    @Test
    void testDetectOS_withMockedSystemProperties() {
        try (MockedStatic<SystemProperties> mocked = Mockito.mockStatic(SystemProperties.class)) {
            mocked.when(SystemProperties::getOsName).thenReturn("Windows 10");
            assertEquals(OperatingSystem.WINDOWS, PlatformDetector.detectOS());

            mocked.when(SystemProperties::getOsName).thenReturn("macOs");
            assertEquals(OperatingSystem.MACOS, PlatformDetector.detectOS());

            mocked.when(SystemProperties::getOsName).thenReturn("Darwin");
            assertEquals(OperatingSystem.MACOS, PlatformDetector.detectOS());

            mocked.when(SystemProperties::getOsName).thenReturn("Linux");
            assertEquals(OperatingSystem.LINUX, PlatformDetector.detectOS());

            mocked.when(SystemProperties::getOsName).thenReturn("UnknownOS");
            assertEquals(OperatingSystem.UNKNOWN, PlatformDetector.detectOS());
        }
    }

    @Test
    void testDetectArch_withMockedSystemProperties() {
        try (MockedStatic<SystemProperties> mocked = Mockito.mockStatic(SystemProperties.class)) {
            mocked.when(SystemProperties::getOsArch).thenReturn("amd64");
            assertEquals(Architecture.X86_64, PlatformDetector.detectArch());

            mocked.when(SystemProperties::getOsArch).thenReturn("aarch64");
            assertEquals(Architecture.AARCH64, PlatformDetector.detectArch());

            mocked.when(SystemProperties::getOsArch).thenReturn("i386");
            assertEquals(Architecture.X86, PlatformDetector.detectArch());

            mocked.when(SystemProperties::getOsArch).thenReturn("armv7");
            assertEquals(Architecture.ARM, PlatformDetector.detectArch());

            mocked.when(SystemProperties::getOsArch).thenReturn("unknown");
            assertEquals(Architecture.UNKNOWN, PlatformDetector.detectArch());
        }
    }
}

