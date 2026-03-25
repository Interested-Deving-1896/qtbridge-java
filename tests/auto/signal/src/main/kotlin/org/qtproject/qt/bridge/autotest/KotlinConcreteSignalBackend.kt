/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest

import org.qtproject.qt.bridge.annotations.QMLRegistrable
import org.qtproject.qt.bridge.annotations.QMLSignal

// Concrete @QMLSignal methods with all three Kotlin visibility levels.
@QMLRegistrable(singleton = true)
open class KotlinConcreteSignalBackend {

    @QMLSignal
    open fun publicSignal(value: Int) {}

    @QMLSignal
    protected open fun protectedSignal(value: Int) {}

    @QMLSignal
    internal  open fun internalSignal(value: Int) {}

    fun triggerPublicSignal(v: Int) {
        publicSignal(v)
    }

    fun triggerProtectedSignal(v: Int) {
        protectedSignal(v)
    }

    fun triggerInternalSignal(v: Int) {
        internalSignal(v)
    }
}
