/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge

interface BackendCallback {
    fun validationError(message: String)
    fun duplicateError(duplicate: String)
    fun operationError(message: String)
    fun fruitAdded(fruitName: String)
    fun fruitUpdated(fruitName: String)
    fun fruitDeleted(fruitName: String)
}
