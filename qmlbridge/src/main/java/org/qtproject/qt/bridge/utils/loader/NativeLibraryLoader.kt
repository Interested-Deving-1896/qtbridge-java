/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.loader

import org.qtproject.qt.bridge.common.QtBridgeConstants
import org.qtproject.qt.bridge.exception.NativeLibraryLoadException
import org.qtproject.qt.bridge.utility.Platform
import org.qtproject.qt.bridge.utils.loader.strategy.ExplicitOverrideStrategy
import org.qtproject.qt.bridge.utils.loader.strategy.JarRelativeStrategy
import org.qtproject.qt.bridge.utils.loader.strategy.LegacyDirectoryStrategy
import org.qtproject.qt.bridge.utils.loader.strategy.LoadResult
import org.qtproject.qt.bridge.utils.loader.strategy.LoadingStrategy
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Native library loader with pluggable loading strategies.
 *
 * Attempts to load native libraries using a configurable chain of strategies in order:
 * 1. **Explicit overrides** - System property or environment variable
 * 2. **JAR-relative** - Libraries alongside the JAR file
 * 3. **Legacy directory** - Backward compatibility fallback
 * 4. **System library path** - Standard JVM library loading
 *
 * ### Configuration
 *
 * Override the default search paths using:
 *  - System property: `-Dqtbridge.native.dir=/path/to/libs`
 *  - Environment variable: `QTBRIDGE_NATIVE_DIR=/path/to/libs`
 *
 * @see LoadingStrategy
 */
internal object NativeLibraryLoader {
    private val loadedLibraries = ConcurrentHashMap.newKeySet<String>()
    private const val LEGACY_DIR = "qmlbridge"

    private val strategies: List<LoadingStrategy> = listOf(
        ExplicitOverrideStrategy(),
        JarRelativeStrategy(NativeLibraryLoader::class.java),
        LegacyDirectoryStrategy(LEGACY_DIR)
    )

    @JvmStatic
    @Synchronized
    fun loadLibrary() {
        loadLibrary(QtBridgeConstants.NATIVE_LIB_NAME)
    }

    @JvmStatic
    @Synchronized
    fun loadLibrary(libName: String) {
        if (loadedLibraries.contains(libName)) {
            return
        }

        Platform.requireSupported()

        val platformDir = Platform.getLibraryDirectory()
        val attempts = mutableListOf<String>()

        // Try each strategy in order
        val libPath = tryStrategies(libName, platformDir, attempts)
        if (libPath != null) {
            loadFromPath(libPath)
            loadedLibraries.add(libName)
        } else {
            loadFromSystem(libName, attempts)
            loadedLibraries.add(libName)
        }
    }

    private fun tryStrategies(
        libName: String,
        platformDir: String,
        attempts: MutableList<String>
    ): Path? {
        for (strategy in strategies) {
            when (val result = strategy.tryLoad(libName, platformDir)) {
                is LoadResult.Success -> {
                    try {
                        loadFromPath(result.libraryPath)
                        return result.libraryPath
                    } catch (e: UnsatisfiedLinkError) {
                        val crashMessage = buildCrashMessage(
                            result.libraryPath,
                            strategy is ExplicitOverrideStrategy,
                            "[UnsatisfiedLinkError]",
                            e.message
                        )
                        attempts.add(crashMessage)
                    } catch (e: LinkageError) {
                        val crashMessage = buildCrashMessage(
                            result.libraryPath,
                            strategy is ExplicitOverrideStrategy,
                            "[LinkageError]",
                            e.message
                        )
                        attempts.add(crashMessage)
                    }
                }
                is LoadResult.Failure -> attempts.add(result.reason)
            }
        }
        return null
    }

    private fun loadFromPath(libPath: Path) {
        val absolutePath = libPath.toAbsolutePath().toString()
        SystemFacade.load(absolutePath)
    }

    private fun loadFromSystem(libName: String, attempts: MutableList<String>) {
        attempts.add("System.loadLibrary($libName)")

        try {
            SystemFacade.loadLibrary(libName)
        } catch (e: UnsatisfiedLinkError) {
            val message = buildErrorMessage(libName, attempts.toList())
            throw NativeLibraryLoadException(libName, message, e)
        }
    }

    private fun buildCrashMessage(
        libraryPath: Path,
        isExplicitOverride: Boolean,
        errorTypeLabel: String,
        errorMessage: String?
    ): String {
        val crashMessage =  buildString {
            append("Found library at $libraryPath")
            if (isExplicitOverride) {
                append(" (explicit override / auto-download)")
            }
            append(" but failed to load ")
            append(errorTypeLabel)
            errorMessage?.let { append(": $it") }
        }
        println("Trying next location...")
        println("   Reason : $crashMessage")
        return crashMessage
    }

    private fun buildErrorMessage(libName: String, attempts: List<String>): String {
        return buildString {
            append("\n\nFailed to load native library '$libName'")
            append(" for platform: ${Platform.getDescription()}")
            append("\n\nAttempted reasons:")
            attempts.forEach { attempt ->
                append("\n  - $attempt")
            }
            append("\n\nMake sure to have Qt libraries in your path!")
            append("\n To specify a custom location for QtBridge library:")
            append("\n  System property: -D${QtBridgeConstants.PROPERTY_NATIVE_DIR}=/path/to/libs")
            append("\n  Environment var: ${QtBridgeConstants.ENV_NATIVE_DIR}=/path/to/libs")
            append("\n\nFor more information, please read documentation on how to build Qt Bridge for java.")
            append("\n\n")
        }
    }

    internal fun resetForTests() {
        loadedLibraries.clear()
    }
}
