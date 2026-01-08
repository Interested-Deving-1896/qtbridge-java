/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

import QtQuick
import QtTest

import QtBridge
import AnotherModule

TestCase {
    name: "Instantiation"
    when: windowShown

    property var singleton1Var : Singleton1.var1
    property var singleton2Var : Singleton2.var1

    SignalSpy {
        id: singleton1Spy
        target: Singleton1
        signalName: "var1Changed"
    }
    SignalSpy {
        id: singleton2Spy
        target: Singleton2
        signalName: "var1Changed"
    }

    function test_singleton_availability() {
        // Test that all singletons are available
        compare(Singleton1.var1, 10);
        compare(Singleton2.var1, 20);
        compare(Singleton1.var1, singleton1Var);
        compare(Singleton2.var1, singleton2Var);

        // Check that their values change independently, first Singleton1
        Singleton1.incrementVar1();
        compare(Singleton1.var1, 11);
        compare(Singleton2.var1, 20);
        singleton1Spy.wait()
        compare(singleton1Spy.count, 1)
        compare(singleton2Spy.count, 0) // unchanged
        compare(Singleton1.var1, singleton1Var);
        compare(Singleton2.var1, singleton2Var);

        // Singleton2
        Singleton2.incrementVar1();
        compare(Singleton1.var1, 11);
        compare(Singleton2.var1, 21);
        singleton2Spy.wait()
        compare(singleton2Spy.count, 1)
        compare(singleton1Spy.count, 1) // unchanged
        compare(Singleton1.var1, singleton1Var);
        compare(Singleton2.var1, singleton2Var);
    }
}
