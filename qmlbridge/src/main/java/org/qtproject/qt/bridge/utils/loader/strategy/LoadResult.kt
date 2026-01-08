/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.loader.strategy

import java.nio.file.Path

internal sealed interface LoadResult {
    data class Success(val libraryPath: Path) : LoadResult
    data class Failure(val reason: String) : LoadResult
}
