/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge

import org.qtproject.qt.bridge.annotations.QMLRegistrable
import org.qtproject.qt.bridge.annotations.QMLSignals
import org.qtproject.qt.bridge.core.QtProperty

interface BaseASignals {
    fun aPing(value: Int)
}

interface BaseBSignals : BaseASignals {
    fun bPing(value: Int)
}

interface TypeCSignals : BaseBSignals {
    fun cPing(value: Int)
}

open class BaseA {
    open val propA = QtProperty(123)
    open fun invokableA(): Int = 123
}

open class BaseB : BaseA() {
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

@QMLRegistrable(includeSuper = false)
class TypeCNoSuper : BaseB() {
    @QMLSignals
    lateinit var signals : TypeCSignals

    val propCNoSuper = QtProperty(789)
    fun invokableCNoSuper(): Int = 789
}

@QMLRegistrable
class TypeCOverride : BaseB() {
    @QMLSignals
    lateinit var signals : TypeCSignals

    val propCOverride = QtProperty(789)

    override val propB = QtProperty(333)
    override val propA = QtProperty(444)
    override fun invokableB(): Int = 111
    override fun invokableA(): Int = 222
}

// Several QMLRegistrables in hierarchy
@QMLRegistrable
open class BaseC : BaseB() {
    open val propC = QtProperty(321)
    open fun invokableC(): Int = 432
}

@QMLRegistrable
open class TypeD : BaseC() {
    open val propD = QtProperty(543)
    open fun invokableD(): Int = 654
}
