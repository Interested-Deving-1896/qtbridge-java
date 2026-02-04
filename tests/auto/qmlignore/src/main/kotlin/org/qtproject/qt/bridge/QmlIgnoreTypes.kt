/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge

import org.qtproject.qt.bridge.annotations.QMLRegistrable
import org.qtproject.qt.bridge.annotations.QMLSignals
import org.qtproject.qt.bridge.annotations.QMLIgnore
import org.qtproject.qt.bridge.core.QtProperty

interface BaseASignals {
    fun aPing(value: Int)
}

interface BaseBSignals : BaseASignals {
    fun bPing(value: Int)
}

interface TypeCSignals : BaseBSignals {
    @QMLIgnore
    fun cPing(value: Int)
}

open class BaseA {
    open val propA = QtProperty(123)
    @QMLIgnore
    open fun invokableA(): Int = 123
}

open class BaseB : BaseA() {
    @QMLIgnore
    open val propB = QtProperty(456)
    open fun invokableB(): Int = 456
}

@QMLRegistrable
class TypeC : BaseB() {
    @QMLSignals
    lateinit var signals : TypeCSignals
    val propC = QtProperty(789)
    fun invokableC(): Int = 789

    fun emitAPing(v: Int) { signals.aPing(v) }
    fun emitBPing(v: Int) { signals.bPing(v) }
    fun emitCPing(v: Int) { signals.cPing(v) }
}
