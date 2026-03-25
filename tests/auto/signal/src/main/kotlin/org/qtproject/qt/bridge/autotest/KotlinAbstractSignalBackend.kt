/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest

import org.qtproject.qt.bridge.annotations.QMLRegistrable
import org.qtproject.qt.bridge.annotations.QMLSignal

// Abstract @QMLSignal methods with all three Kotlin visibility levels.
// Due to these abstract methods, the class itself must also be abstract
@QMLRegistrable(singleton = true)
abstract class KotlinAbstractSignalBackend {

    @QMLSignal
    abstract fun publicSignal(value: Int)

    @QMLSignal
    protected abstract fun protectedSignal(value: Int)

    @QMLSignal
    internal  abstract fun internalSignal(value: Int)

    fun triggerPublicSignal(v: Int) {
        publicSignal(v)
    }

    fun triggerProtectedSignal(v: Int) {
        protectedSignal(v)
    }

    fun triggerInternalSignal(v: Int)  {
        internalSignal(v)
    }
}
