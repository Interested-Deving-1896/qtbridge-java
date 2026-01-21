/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.resolver

import java.io.File


// Resolves Qt tools, directories, paths to Qt resources such as libraries or bridge native
internal interface QtFileResolver {
    fun resolve(): File?
}
