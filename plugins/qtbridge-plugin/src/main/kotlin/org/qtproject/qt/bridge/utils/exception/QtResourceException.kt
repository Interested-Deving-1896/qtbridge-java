/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.exception

import org.gradle.api.GradleException
import java.io.File

internal sealed class QtResourceException(
    message: String,
    cause: Throwable? = null
) : GradleException(message, cause)

internal class QtResourceDownloadException(
    description:String,
    cause: Throwable? = null
) : QtResourceException("Failed to download $description", cause)

internal class QtResourceExtractionException(
    val file: File,
    cause: Throwable? = null
) : QtResourceException("Failed to extract from ${file.name}", cause)
