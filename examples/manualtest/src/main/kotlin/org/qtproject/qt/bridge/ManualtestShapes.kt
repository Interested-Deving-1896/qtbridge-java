/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge

import org.qtproject.qt.bridge.annotations.QMLRegistrable
import org.qtproject.qt.bridge.annotations.QMLSignals
import org.qtproject.qt.bridge.core.QtProperty

interface ShapeSignals {
    fun area(value: Int)
}

open class Shape {
    @QMLSignals lateinit var signals: ShapeSignals
    open fun area() : Int { return 1; }
}

@QMLRegistrable(module = "InheritingTypes")
open class Circle : Shape() {
    override fun area() : Int { return 2; }
    fun radius() : Int { return 3; }
}

@QMLRegistrable(module = "InheritingTypes")
open class Triangle: Shape() {
    override fun area() : Int { return 4; }
    fun sideLength() : Int { return 5; }
}

@QMLRegistrable(module = "InheritingTypes")
open class RoundedTriangle: Triangle() {
    fun cornerRadius() : Int { return 6; }
}
