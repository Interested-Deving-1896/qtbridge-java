/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

import QtQuick
import QtTest

import QtBridge

TestCase {
    id: tc
    name: "Inheritance"
    when: windowShown

    // Type defined in Java
    TypeA { id: a }

    // Types defined in Kotlin
    TypeC { id: c }
    TypeCNoSuper { id: c2 }
    TypeCOverride { id: c3 }
    BaseC { id: baseC }
    TypeD { id: typeD }

    SignalSpy { id: spyA; target: c; signalName: "aPing" }
    SignalSpy { id: spyB; target: c; signalName: "bPing" }
    SignalSpy { id: spyC; target: c; signalName: "cPing" }

    function test_properties()
    {
        compare(a.propBase, 123)
        compare(a.propA, 456)

        compare(c.propC, 789)
        compare(c.propB, 456)
        compare(c.propA, 123)

        // Types that have multiple QMLRegistrables in hierarchy
        compare(baseC.propC, 321)
        compare(typeD.propD, 543)

        // Check that 'includeSuper == false' is effective
        compare(c2.propCNoSuper, 789)
        verify(c2.propB === undefined)
        verify(c2.propA === undefined)

        // Check that overriding virtual properties works
        verify(c3.propB, 333)
        verify(c3.propA, 444)
    }

    function test_invokables()
    {
        compare(a.invokableA(), 456)
        compare(a.invokableBase(), 123)

        compare(c.invokableC(), 789)
        compare(c.invokableB(), 456)
        compare(c.invokableA(), 123)

        // Types that have multiple @QMLRegistrables in hierarchy
        compare(baseC.invokableC(), 432)
        compare(typeD.invokableD(), 654)

        // Check that 'includeSuper == false' is effective
        compare(c2.invokableCNoSuper(), 789)
        verify(c2.invokableB === undefined)
        verify(c2.invokableA === undefined)

        // Check that overriding virtual methods works
        compare(c3.propCOverride, 789)
        compare(c3.invokableB(), 111)
        compare(c3.invokableA(), 222)
    }

    function test_signals()
    {
        c.emitAPing(123)
        tryCompare(spyA, "count", 1)
        compare(spyB.count, 0)
        compare(spyC.count, 0)
        compare(spyA.signalArguments[0][0], 123)

        c.emitBPing(456)
        tryCompare(spyB, "count", 1)
        compare(spyA.count, 1)
        compare(spyC.count, 0)
        compare(spyB.signalArguments[0][0], 456)

        c.emitCPing(789)
        tryCompare(spyC, "count", 1)
        compare(spyA.count, 1)
        compare(spyB.count, 1)
        compare(spyC.signalArguments[0][0], 789)
    }
}
