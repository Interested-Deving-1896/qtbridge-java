/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class PlatformTest {

    @BeforeEach
    void resetPlatform() {
        Platform.reset();
    }

    @Test
    void testWindowsX86_64() {
        try (MockedStatic<PlatformDetector> mocked = Mockito.mockStatic(PlatformDetector.class)) {
            mocked.when(PlatformDetector::detectOS).thenReturn(OperatingSystem.WINDOWS);
            mocked.when(PlatformDetector::detectArch).thenReturn(Architecture.X86_64);
            assertTrue(Platform.isWindows());
            assertFalse(Platform.isLinux());
            assertTrue(Platform.isX86_64());
            assertTrue(Platform.is64Bit());
            assertEquals(".dll", Platform.getSharedLibrarySuffix());
            assertEquals("win_amd64", Platform.getLibraryDirectory());
        }
    }

    @Test
    void testMacOSAarch64() {
        try (MockedStatic<PlatformDetector> mocked = Mockito.mockStatic(PlatformDetector.class)) {
            mocked.when(PlatformDetector::detectOS).thenReturn(OperatingSystem.MACOS);
            mocked.when(PlatformDetector::detectArch).thenReturn(Architecture.AARCH64);
            assertTrue(Platform.isMacOS());
            assertFalse(Platform.isWindows());
            assertTrue(Platform.isAarch64());
            assertTrue(Platform.is64Bit());
            assertEquals(".dylib", Platform.getSharedLibrarySuffix());
            assertEquals("macos_arm64", Platform.getLibraryDirectory());
        }
    }

    @Test
    void testUnsupportedPlatformThrows() {
        try (MockedStatic<PlatformDetector> mocked = Mockito.mockStatic(PlatformDetector.class)) {
            mocked.when(PlatformDetector::detectOS).thenReturn(OperatingSystem.UNKNOWN);
            mocked.when(PlatformDetector::detectArch).thenReturn(Architecture.UNKNOWN);
            assertFalse(Platform.isSupported());
            assertThrows(UnsupportedPlatformException.class, Platform::requireSupported);
        }
    }
}
