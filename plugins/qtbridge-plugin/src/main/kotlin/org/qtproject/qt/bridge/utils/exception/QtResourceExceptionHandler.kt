/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.exception

import org.gradle.api.logging.Logger

internal class QtResourceExceptionHandler(private val logger: Logger) {

    private data class ErrorAnalysis(
        val title: String,
        val solutions: List<String>
    )

    fun handle(e: Exception) {
        val analysis = analyze(e)

        logErrorHeader(analysis.title)
        logDetails(e.message)
        logSolutions(analysis.solutions)
    }

    private fun analyze(e: Exception): ErrorAnalysis {
        return when (e) {
            is QtResourceDownloadException -> ErrorAnalysis(
                title = "Dependency Download Failed",
                solutions = listOf(
                    "Check your internet connection.",
                    "Check Qt Bridge documentation of supported Operating systems and architectures",
                    "Verify that the requested version exists in the remote repository",
                )
            )

            is QtResourceExtractionException -> ErrorAnalysis(
                title = "Archive Extraction Failed",
                solutions = listOf(
                    "Ensure you have sufficient disk space.",
                    "Check if the file is corrupted by deleting the cache manually in ~/.gradle/caches/qt-downloads.",
                    "Run './gradlew clean' to clear previous artifacts."
                )
            )

            is QtResourceException -> ErrorAnalysis(
                title = "QtBridge Resource Error",
                solutions = listOf(
                    "Read the error message above for specific details.",
                    "Check your 'qtBridge' configuration block in build.gradle.kts."
                )
            )

            else -> ErrorAnalysis(
                title = "Unexpected Internal Error",
                solutions = listOf(
                    "Please report this issue to the QtBridge maintainers",
                    "Run with --stacktrace to see the full internal error.",
                    "Check if your Gradle version is compatible."
                )
            )
        }
    }

    private fun logErrorHeader(title: String) {
        logger.error("")
        logger.error("  QtBridge Error: $title")
        logger.error("")
    }

    private fun logDetails(message: String?) {
        logger.error("   Details: ${message ?: "No specific error message provided."}")
        logger.error("")
    }

    private fun logSolutions(solutions: List<String>) {
        logger.error("   Possible Solutions:")
        solutions.forEach { solution ->
            logger.error("      • $solution")
        }
        logger.error("")
    }
}
