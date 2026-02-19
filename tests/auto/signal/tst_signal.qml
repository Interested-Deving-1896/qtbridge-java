/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

import QtQuick
import QtTest

import QtBridge

Item {
    id: root

    property var targetBackend

    InstantiableTestBackend {
        id: instantiableTestBackend
    }

    SignalSpy {
        id: onCallFunctionSpy
        target: root.targetBackend
        signalName: "onCall"
    }

    SignalSpy {
        id: onCallIntFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithInt"
    }

    SignalSpy {
        id: onCallIntegerFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithInteger"
    }

    SignalSpy {
        id: onCallBooleanFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithBoolean"
    }

    SignalSpy {
        id: onCallBooleanObjectFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithBooleanObject"
    }

    SignalSpy {
        id: onCallDoubleFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithDouble"
    }

    SignalSpy {
        id: onCallDoubleObjectFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithDoubleObject"
    }

     SignalSpy {
        id: onCallLongFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithLong"
    }

    SignalSpy {
        id: onCallLongObjectFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithLongObject"
    }

    SignalSpy {
        id: onCallFloatFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithFloat"
    }

    SignalSpy {
        id: onCallFloatObjectFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithFloatObject"
    }
    SignalSpy {
        id: onCallCharFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithChar"
    }

    SignalSpy {
        id: onCallCharObjectFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithCharObject"
    }

    SignalSpy {
        id: onCallWithTwoIntsFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithTwoInts"
    }

    SignalSpy {
        id: onCallWithThreeIntsFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithThreeInts"
    }

    SignalSpy {
        id: onCallWithIntAndStringFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithIntAndString"
    }

    SignalSpy {
        id: onCallWithBooleanAndDoubleFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithBooleanAndDouble"
    }

     SignalSpy {
        id: onCallWithStringFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithString"
    }

    SignalSpy {
        id: onCallWithListFunctionSpy
        target: root.targetBackend
        signalName: "onCallWithList"
    }

    SignalSpy {
        id: onCallWithMapSpy
        target: root.targetBackend
        signalName: "onCallWithMap"
    }

    SignalSpy {
        id: onCallWithUrlSpy
        target: root.targetBackend
        signalName: "onCallWithUrl"
    }

    SignalSpy {
        id: onCallWithEnumSpy
        target: root.targetBackend
        signalName: "onCallWithEnum"
    }

    TestCase {
        name: "Signals"
        when: windowShown

        // Run the same test functions for both instantiable and singleton instances.
        // Each test function sets the root.targetBackend property based on the
        // init_data, and all signal spies are bound to that.
        function init_data() {
            return [
                { tag: "singleton",    target: SingletonTestBackend },
                { tag: "instantiable", target: instantiableTestBackend }
            ]
        }

        function test_call(data) {
            root.targetBackend = data.target
            onCallFunctionSpy.clear()
            root.targetBackend.triggerCall()
            wait(1)
            compare(onCallFunctionSpy.count, 1)
        }

        function test_callInt(data) {
            root.targetBackend = data.target
            onCallIntFunctionSpy.clear()
            root.targetBackend.triggerCallWithInt(5)
            wait(1)
            compare(onCallIntFunctionSpy.count, 1)
            compare(onCallIntFunctionSpy.signalArguments[0].length, 1)
            compare(onCallIntFunctionSpy.signalArguments[0][0], 5)
        }

        function test_callInteger(data) {
            root.targetBackend = data.target
            onCallIntegerFunctionSpy.clear()
            root.targetBackend.triggerCallWithInteger(-5)
            wait(1)
            compare(onCallIntegerFunctionSpy.count, 1)
            compare(onCallIntegerFunctionSpy.signalArguments[0].length, 1)
            compare(onCallIntegerFunctionSpy.signalArguments[0][0], -5)
        }

        function test_callBoolTrue(data) {
            root.targetBackend = data.target
            onCallBooleanFunctionSpy.clear()
            root.targetBackend.triggerCallWithBoolean(true)
            wait(1)
            compare(onCallBooleanFunctionSpy.count, 1)
            compare(onCallBooleanFunctionSpy.signalArguments[0].length, 1)
            compare(onCallBooleanFunctionSpy.signalArguments[0][0], true)
        }

        function test_callBoolFalse(data) {
            root.targetBackend = data.target
            onCallBooleanFunctionSpy.clear()
            root.targetBackend.triggerCallWithBoolean(false)
            wait(1)
            compare(onCallBooleanFunctionSpy.count, 1)
            compare(onCallBooleanFunctionSpy.signalArguments[0].length, 1)
            compare(onCallBooleanFunctionSpy.signalArguments[0][0], false)
        }

        function test_callBoolean(data) {
            root.targetBackend = data.target
            onCallBooleanObjectFunctionSpy.clear()
            root.targetBackend.triggerCallWithBooleanObject(true)
            wait(1)
            compare(onCallBooleanObjectFunctionSpy.count, 1)
            compare(onCallBooleanObjectFunctionSpy.signalArguments[0].length, 1)
            compare(onCallBooleanObjectFunctionSpy.signalArguments[0][0], true)
        }

        function test_callDouble(data) {
            root.targetBackend = data.target
            onCallDoubleFunctionSpy.clear()
            root.targetBackend.triggerCallWithDouble(.4)
            wait(1)
            compare(onCallDoubleFunctionSpy.count, 1)
            compare(onCallDoubleFunctionSpy.signalArguments[0].length, 1)
            compare(onCallDoubleFunctionSpy.signalArguments[0][0], 0.4)
        }

        function test_callDoubleObject(data) {
            root.targetBackend = data.target
            onCallDoubleObjectFunctionSpy.clear()
            root.targetBackend.triggerCallWithDoubleObject(-.3)
            wait(1)
            compare(onCallDoubleObjectFunctionSpy.count, 1)
            compare(onCallDoubleObjectFunctionSpy.signalArguments[0].length, 1)
            compare(onCallDoubleObjectFunctionSpy.signalArguments[0][0], -.3)
        }

        function test_callLong(data) {
            root.targetBackend = data.target
            onCallLongFunctionSpy.clear()
            root.targetBackend.triggerCallWithLong(100000002)
            wait(1)
            compare(onCallLongFunctionSpy.count, 1)
            compare(onCallLongFunctionSpy.signalArguments[0].length, 1)
            compare(onCallLongFunctionSpy.signalArguments[0][0], 100000002)
        }

        function test_callLongObject(data) {
            root.targetBackend = data.target
            onCallLongObjectFunctionSpy.clear()
            root.targetBackend.triggerCallWithLongObject(100000002)
            wait(1)
            compare(onCallLongObjectFunctionSpy.count, 1)
            compare(onCallLongObjectFunctionSpy.signalArguments[0].length, 1)
            compare(onCallLongObjectFunctionSpy.signalArguments[0][0], 100000002)
        }

        function test_callFloat(data) {
            root.targetBackend = data.target
            onCallFloatFunctionSpy.clear()
            root.targetBackend.triggerCallWithFloat(.4)
            wait(1)
            compare(onCallFloatFunctionSpy.count, 1)
            compare(onCallFloatFunctionSpy.signalArguments[0].length, 1)
            compare(onCallFloatFunctionSpy.signalArguments[0][0], 0.4)
        }

        function test_callFloatObject(data) {
            root.targetBackend = data.target
            onCallFloatObjectFunctionSpy.clear()
            root.targetBackend.triggerCallWithFloatObject(-.34)
            wait(1)
            compare(onCallFloatObjectFunctionSpy.count, 1)
            compare(onCallFloatObjectFunctionSpy.signalArguments[0].length, 1)
            compare(onCallFloatObjectFunctionSpy.signalArguments[0][0], -.34)
        }

        function test_callChar(data) {
            root.targetBackend = data.target
            onCallCharFunctionSpy.clear()
            root.targetBackend.triggerCallWithChar('q')
            wait(1)
            compare(onCallCharFunctionSpy.count, 1)
            compare(onCallCharFunctionSpy.signalArguments[0].length, 1)
            compare(onCallCharFunctionSpy.signalArguments[0][0], 'q')
        }

        function test_callCharObject(data) {
            root.targetBackend = data.target
            onCallCharObjectFunctionSpy.clear()
            root.targetBackend.triggerCallWithCharObject('t')
            wait(1)
            compare(onCallCharObjectFunctionSpy.count, 1)
            compare(onCallCharObjectFunctionSpy.signalArguments[0].length, 1)
            compare(onCallCharObjectFunctionSpy.signalArguments[0][0], 't')
        }

        function test_callWithTwoInts(data) {
            root.targetBackend = data.target
            onCallWithTwoIntsFunctionSpy.clear()
            root.targetBackend.triggerCallWithTwoInts(-3, 4)
            wait(1)
            compare(onCallWithTwoIntsFunctionSpy.count, 1)
            compare(onCallWithTwoIntsFunctionSpy.signalArguments[0].length, 2)
            compare(onCallWithTwoIntsFunctionSpy.signalArguments[0][1], 4)
            compare(onCallWithTwoIntsFunctionSpy.signalArguments[0][0], -3)
        }

        function test_callWithThreeInts(data) {
            root.targetBackend = data.target
            onCallWithThreeIntsFunctionSpy.clear()
            root.targetBackend.triggerCallWithThreeInts(-3, 4,5 )
            wait(1)
            compare(onCallWithThreeIntsFunctionSpy.count, 1)
            compare(onCallWithThreeIntsFunctionSpy.signalArguments[0].length, 3)
        }

        function test_callWithIntAndString(data) {
            root.targetBackend = data.target
            onCallWithIntAndStringFunctionSpy.clear()
            root.targetBackend.triggerCallWithIntAndString(33333,"qt")
            wait(1)
            compare(onCallWithIntAndStringFunctionSpy.count, 1)
            compare(onCallWithIntAndStringFunctionSpy.signalArguments[0].length, 2)
            compare(onCallWithIntAndStringFunctionSpy.signalArguments[0][0], 33333)
            compare(onCallWithIntAndStringFunctionSpy.signalArguments[0][1], "qt")
        }

        function test_callWithBooleanAndDouble(data) {
            root.targetBackend = data.target
            onCallWithBooleanAndDoubleFunctionSpy.clear()
            root.targetBackend.triggerCallWithBooleanAndDouble(true,3.14)
            wait(1)
            compare(onCallWithBooleanAndDoubleFunctionSpy.count, 1)
            compare(onCallWithBooleanAndDoubleFunctionSpy.signalArguments[0].length, 2)
            compare(onCallWithBooleanAndDoubleFunctionSpy.signalArguments[0][0], true)
            compare(onCallWithBooleanAndDoubleFunctionSpy.signalArguments[0][1], 3.14)
        }

        function test_callWithString(data) {
            root.targetBackend = data.target
            onCallWithStringFunctionSpy.clear()
            root.targetBackend.triggerCallWithString("qt")
            wait(1)
            compare(onCallWithStringFunctionSpy.count, 1)
            compare(onCallWithStringFunctionSpy.signalArguments[0].length, 1)
            compare(onCallWithStringFunctionSpy.signalArguments[0][0], "qt")
        }

        function test_callWithList(data) {
            root.targetBackend = data.target
            onCallWithListFunctionSpy.clear()
            var myList = ["apple", "banana", "cherry"];
            root.targetBackend.triggerCallWithList(myList)
            wait(1)
            compare(onCallWithListFunctionSpy.count, 1)
            compare(onCallWithListFunctionSpy.signalArguments[0].length, 1)
            var listArg = onCallWithListFunctionSpy.signalArguments[0][0]
            compare(listArg[0], "apple")
            compare(listArg[1], "banana")
            compare(listArg[2], "cherry")
        }

        function test_callWithMap(data) {
            root.targetBackend = data.target
            onCallWithMapSpy.clear()
            var myMap = {
                k1: 1,
                k2: "two",
                k3: 3.141,
                a1: [
                    { k2: 2 },
                    { k3: 3 }
                ],
                o1: {
                    k4: 4,
                    a2: [1, 2, 3]
                }
            }
            root.targetBackend.triggerCallWithMap(myMap)
            wait(1)
            compare(onCallWithMapSpy.count, 1)
            compare(onCallWithMapSpy.signalArguments[0].length, 1)
            // Verify that original and received signal argument match
            var mapArg = onCallWithMapSpy.signalArguments[0][0]
            compare(mapArg, myMap)
        }

        function test_callWithUrl(data) {
            root.targetBackend = data.target
            onCallWithUrlSpy.clear()
            var myUrl = "http://example.com/path"
            root.targetBackend.triggerCallWithUrl(myUrl);
            wait(1)
            compare(onCallWithUrlSpy.count, 1)
            compare(onCallWithUrlSpy.signalArguments[0].length, 1)
            // Verify that original and received signal argument match
            var urlArg = onCallWithUrlSpy.signalArguments[0][0]
            compare(urlArg, myUrl)
        }

        function test_callWithEnum(data) {
            root.targetBackend = data.target
            onCallWithEnumSpy.clear()
            // Calling without parameters because we can't
            // currently use enums as parmeters (QML => Java), see QTBUG-141710
            root.targetBackend.triggerCallWithEnum();
            wait(1)
            compare(onCallWithEnumSpy.count, 1)
            compare(onCallWithEnumSpy.signalArguments[0].length, 1)
            // Verify that original and received signal argument match
            var enumArg = onCallWithEnumSpy.signalArguments[0][0]
            compare(enumArg.name, "BLUE")
            compare(enumArg.ordinal, 2)
            compare(enumArg.rgb, "#0000FF")
        }

        function test_callbackProxyToString(data) {
            // Test QtSignalProxy.toString() works (does not try to emit)
            root.targetBackend = data.target
            let string = root.targetBackend.callbackProxyToString()
            verify(string.length > 0)
        }
    }
}
