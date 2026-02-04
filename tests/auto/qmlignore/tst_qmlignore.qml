/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

import QtQuick
import QtTest

import QtBridge

TestCase {
    id: tc
    name: "QmlIgnore"
    when: windowShown

    TypeC { id: typeC }

    SignalSpy { id: spyA; target: typeC; signalName: "aPing" }
    SignalSpy { id: spyB; target: typeC; signalName: "bPing" }
    SignalSpy { id: spyC; target: typeC; signalName: "cPing" }

    function test_properties()
    {
        compare(typeC.propC, 789)
        verify(typeC.propB === undefined) // propB is QMLIgnored
        compare(typeC.propA, 123)
    }

    function test_invokables()
    {
        compare(typeC.invokableC(), 789)
        compare(typeC.invokableB(), 456)
        verify(typeC.invokableA === undefined) // invokableA() is QMLIgnored
    }

    function test_signals()
    {
        typeC.emitAPing(111)
        typeC.emitBPing(222)

        ignoreWarning(new RegExp("Cache entry not found for signal"))
        typeC.emitCPing(333) // signal is QMLIgnored
        wait(1)
        compare(spyA.count, 1)
        compare(spyB.count, 1)
        compare(spyC.count, 0)
    }
}
