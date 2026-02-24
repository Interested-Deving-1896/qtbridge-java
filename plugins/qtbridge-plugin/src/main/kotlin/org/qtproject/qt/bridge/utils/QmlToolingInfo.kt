/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils

import groovy.json.JsonOutput
import java.io.File

// Dependencies likely need changes when we implement QTBUG-143107,
internal data class QmlToolingInfo(
    val qmllsPath: String? = null,
    val qmllintPath: String? = null,
    val qmllsIniFilePath: String? = null,
    val buildDir: String? = null,
    val docDir: String? = null,
    val qmlImportPaths: List<String> = emptyList(),
    val qmlResourcesPaths: List<String> = emptyList(),
) {
    fun toJson(): String {
        val jsonString = JsonOutput.toJson(this)
        return JsonOutput.prettyPrint(jsonString)
    }

    fun writeToFile(file: File) {
        file.parentFile?.mkdirs()
        file.writeText(toJson())
    }

    companion object {
        private const val FILENAME = "qmltools.json"
        private const val STANDARD_LOCATION = ".ide_tools"

        fun getStandardFile(projectDir: File) = File(projectDir, "$STANDARD_LOCATION/$FILENAME")
    }
}
