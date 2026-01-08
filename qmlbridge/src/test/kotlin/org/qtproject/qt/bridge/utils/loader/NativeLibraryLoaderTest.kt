/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.loader

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.qtproject.qt.bridge.exception.NativeLibraryLoadException
import org.qtproject.qt.bridge.utility.Platform
import org.qtproject.qt.bridge.utils.loader.strategy.ExplicitOverrideStrategy
import org.qtproject.qt.bridge.utils.loader.strategy.JarRelativeStrategy
import org.qtproject.qt.bridge.utils.loader.strategy.LegacyDirectoryStrategy
import org.qtproject.qt.bridge.utils.loader.strategy.LoadResult
import java.nio.file.Path
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NativeLibraryLoaderTest {

    @BeforeEach
    fun setup() {
        mockkStatic(Platform::class)
        mockkObject(SystemFacade)
        mockkConstructor(ExplicitOverrideStrategy::class)
        mockkConstructor(JarRelativeStrategy::class)
        mockkConstructor(LegacyDirectoryStrategy::class)
        every { Platform.requireSupported() } just Runs
        every { Platform.getLibraryDirectory() } returns "macos_arm64"
        every { Platform.getDescription() } returns "macOS aarch64"
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        clearAllMocks()
        NativeLibraryLoader.resetForTests()
    }

    @Test
    fun `loads from ExplicitOverride strategy`() {
        val libPath = Path.of("/tmp/libTest.dylib")
        every { SystemFacade.load(any()) } just Runs
        every { anyConstructed<ExplicitOverrideStrategy>().tryLoad(any(), any()) } returns LoadResult.Success(libPath)
        every { anyConstructed<JarRelativeStrategy>().tryLoad(any(), any()) } returns LoadResult.Success(libPath)
        every { anyConstructed<LegacyDirectoryStrategy>().tryLoad(any(), any()) } returns LoadResult.Success(libPath)
        NativeLibraryLoader.loadLibrary("Test")
    }

    @Test
    fun `falls back to next strategy if first fails`() {
        val libPath = Path.of("/usr/local/lib/libTest.dylib")
        every { SystemFacade.load(any()) } just Runs
        every { anyConstructed<ExplicitOverrideStrategy>().tryLoad(any(), any()) } returns LoadResult.Failure("not found")
        every { anyConstructed<JarRelativeStrategy>().tryLoad(any(), any()) } returns LoadResult.Success(libPath)
        NativeLibraryLoader.loadLibrary("Test")
    }

    @Test
    fun `throws exception when all strategies fail`() {
        mockkConstructor(ExplicitOverrideStrategy::class)
        mockkConstructor(JarRelativeStrategy::class)
        mockkConstructor(LegacyDirectoryStrategy::class)

        every { anyConstructed<ExplicitOverrideStrategy>().tryLoad(any(), any()) } returns LoadResult.Failure("missing")
        every { anyConstructed<JarRelativeStrategy>().tryLoad(any(), any()) } returns LoadResult.Failure("not found")
        every { anyConstructed<LegacyDirectoryStrategy>().tryLoad(any(), any()) } returns LoadResult.Failure("not on classpath")

        val ex = assertFailsWith<NativeLibraryLoadException> {
            NativeLibraryLoader.loadLibrary("Test")
        }

        assertTrue(ex.message!!.contains("missing"))
        verify(exactly = 0) { SystemFacade.load(any()) }
    }

    @Test
    fun `repeated loadLibrary calls only load once`() {
        val libPath = Path.of("/tmp/libRepeat.dylib")
        every { SystemFacade.load(any()) } just Runs
        mockkConstructor(ExplicitOverrideStrategy::class)
        every { anyConstructed<ExplicitOverrideStrategy>().tryLoad(any(), any()) } returns LoadResult.Success(libPath)

        NativeLibraryLoader.loadLibrary("Test")
        NativeLibraryLoader.loadLibrary("Test")

        verify(exactly = 1) { SystemFacade.load(libPath.toAbsolutePath().toString()) }
    }

    @Test
    fun `requires supported platform before loading`() {
        every { Platform.requireSupported() } throws UnsupportedOperationException("Not supported")
        val ex = assertFailsWith<UnsupportedOperationException> {
            NativeLibraryLoader.loadLibrary("Test")
        }
        assertTrue(ex.message!!.contains("Not supported"))
        verify(exactly = 1) { Platform.requireSupported() }
    }
}
