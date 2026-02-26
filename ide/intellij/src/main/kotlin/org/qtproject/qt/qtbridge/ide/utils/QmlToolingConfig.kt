/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.utils

import java.io.File

internal data class QmlToolingConfig(
    val qmllsPath: String? = null,
    val qmllintPath: String? = null,
    val qmllsIniFilePath: String? = null,
    val buildDir: String? = null,
    val docDir: String? = null,
    val qmlImportPaths: List<String> = emptyList(),
    val qmlResourcesPaths: List<String> = emptyList(),
)

internal data class QmlLintConfig(
    val qmlFilePath: String,
    val qmllintPath: String,
    val qmlImportPaths: List<String> = emptyList(),
    val qmlResourcesPaths: List<String> = emptyList(),
)

internal data class QmlLSConfig(
    val qmllsPath: String,
    val buildDir: String,
    val docDir:String,
)

internal fun QmlToolingConfig.toQmlLintConfig(qmlFile: File): QmlLintConfig {
    return QmlLintConfig(
        qmlFilePath = qmlFile.absolutePath,
        qmllintPath = this.qmllintPath!!,
        qmlImportPaths = this.qmlImportPaths,
        qmlResourcesPaths = this.qmlResourcesPaths
    )
}

internal fun QmlToolingConfig.toQmlLSConfig(): QmlLSConfig {
    return QmlLSConfig(
        qmllsPath = this.qmllsPath ?: "",
        buildDir = this.buildDir ?: "",
        docDir = this.docDir ?: ""
    )
}

internal data class GradleKspConfig(val projectPath: File, val kspTask: String = ":kspKotlin")
