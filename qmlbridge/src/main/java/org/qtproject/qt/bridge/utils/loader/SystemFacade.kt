/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils.loader

// this is introduced to be able to mock it in test as (System cannot be mocked with mockito)
internal object SystemFacade {
    fun loadLibrary(libName: String) = System.loadLibrary(libName)
    fun load(fileName: String) = System.load(fileName)
}
