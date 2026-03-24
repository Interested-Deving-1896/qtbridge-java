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

    // @QMLSignal method signal spies
    SignalSpy {
        id: signalNoParamsSpy
        target: root.targetBackend
        signalName: "onSignalNoParams"
    }

    SignalSpy {
        id: signalWithIntSpy
        target: root.targetBackend
        signalName: "onSignalWithInt"
    }

    SignalSpy {
        id: signalWithIntegerSpy
        target: root.targetBackend
        signalName: "onSignalWithInteger"
    }

    SignalSpy {
        id: signalWithBooleanSpy
        target: root.targetBackend
        signalName: "onSignalWithBoolean"
    }

    SignalSpy {
        id: signalWithBooleanObjectSpy
        target: root.targetBackend
        signalName: "onSignalWithBooleanObject"
    }

    SignalSpy {
        id: signalWithDoubleSpy
        target: root.targetBackend
        signalName: "onSignalWithDouble"
    }

    SignalSpy {
        id: signalWithDoubleObjectSpy
        target: root.targetBackend
        signalName: "onSignalWithDoubleObject"
    }

    SignalSpy {
        id: signalWithLongSpy
        target: root.targetBackend
        signalName: "onSignalWithLong"
    }

    SignalSpy {
        id: signalWithLongObjectSpy
        target: root.targetBackend
        signalName: "onSignalWithLongObject"
    }

    SignalSpy {
        id: signalWithFloatSpy
        target: root.targetBackend
        signalName: "onSignalWithFloat"
    }

    SignalSpy {
        id: signalWithFloatObjectSpy
        target: root.targetBackend
        signalName: "onSignalWithFloatObject"
    }

    SignalSpy {
        id: signalWithCharSpy
        target: root.targetBackend
        signalName: "onSignalWithChar"
    }

    SignalSpy {
        id: signalWithCharObjectSpy
        target: root.targetBackend
        signalName: "onSignalWithCharObject"
    }

    SignalSpy {
        id: signalWithTwoIntsSpy
        target: root.targetBackend
        signalName: "onSignalWithTwoInts"
    }

    SignalSpy {
        id: signalWithThreeIntsSpy
        target: root.targetBackend
        signalName: "onSignalWithThreeInts"
    }

    SignalSpy {
        id: signalWithIntAndStringSpy
        target: root.targetBackend
        signalName: "onSignalWithIntAndString"
    }

    SignalSpy {
        id: signalWithBooleanAndDoubleSpy
        target: root.targetBackend
        signalName: "onSignalWithBooleanAndDouble"
    }

    SignalSpy {
        id: signalWithStringSpy
        target: root.targetBackend
        signalName: "onSignalWithString"
    }

    SignalSpy {
        id: signalWithListSpy
        target: root.targetBackend
        signalName: "onSignalWithList"
    }

    SignalSpy {
        id: signalWithMapSpy
        target: root.targetBackend
        signalName: "onSignalWithMap"
    }

    SignalSpy {
        id: signalWithUrlSpy
        target: root.targetBackend
        signalName: "onSignalWithUrl"
    }

    SignalSpy {
        id: signalWithEnumSpy
        target: root.targetBackend
        signalName: "onSignalWithEnum"
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
            signalNoParamsSpy.clear()
            root.targetBackend.triggerCall()
            wait(1)
            compare(onCallFunctionSpy.count, 1)
            compare(signalNoParamsSpy.count, 1)
            compare(signalNoParamsSpy.signalArguments[0].length, 0)
        }

        function test_callInt(data) {
            root.targetBackend = data.target
            onCallIntFunctionSpy.clear()
            signalWithIntSpy.clear()
            root.targetBackend.triggerCallWithInt(5)
            wait(1)
            compare(onCallIntFunctionSpy.count, 1)
            compare(onCallIntFunctionSpy.signalArguments[0].length, 1)
            compare(onCallIntFunctionSpy.signalArguments[0][0], 5)
            compare(signalWithIntSpy.count, 1)
            compare(signalWithIntSpy.signalArguments[0].length, 1)
            compare(signalWithIntSpy.signalArguments[0][0], 5)
        }

        function test_callInteger(data) {
            root.targetBackend = data.target
            onCallIntegerFunctionSpy.clear()
            signalWithIntegerSpy.clear()
            root.targetBackend.triggerCallWithInteger(-5)
            wait(1)
            compare(onCallIntegerFunctionSpy.count, 1)
            compare(onCallIntegerFunctionSpy.signalArguments[0].length, 1)
            compare(onCallIntegerFunctionSpy.signalArguments[0][0], -5)
            compare(signalWithIntegerSpy.count, 1)
            compare(signalWithIntegerSpy.signalArguments[0].length, 1)
            compare(signalWithIntegerSpy.signalArguments[0][0], -5)
        }

        function test_callBoolTrue(data) {
            root.targetBackend = data.target
            onCallBooleanFunctionSpy.clear()
            signalWithBooleanSpy.clear()
            root.targetBackend.triggerCallWithBoolean(true)
            wait(1)
            compare(onCallBooleanFunctionSpy.count, 1)
            compare(onCallBooleanFunctionSpy.signalArguments[0].length, 1)
            compare(onCallBooleanFunctionSpy.signalArguments[0][0], true)
            compare(signalWithBooleanSpy.count, 1)
            compare(signalWithBooleanSpy.signalArguments[0].length, 1)
            compare(signalWithBooleanSpy.signalArguments[0][0], true)
        }

        function test_callBoolFalse(data) {
            root.targetBackend = data.target
            onCallBooleanFunctionSpy.clear()
            signalWithBooleanSpy.clear()
            root.targetBackend.triggerCallWithBoolean(false)
            wait(1)
            compare(onCallBooleanFunctionSpy.count, 1)
            compare(onCallBooleanFunctionSpy.signalArguments[0].length, 1)
            compare(onCallBooleanFunctionSpy.signalArguments[0][0], false)
            compare(signalWithBooleanSpy.count, 1)
            compare(signalWithBooleanSpy.signalArguments[0].length, 1)
            compare(signalWithBooleanSpy.signalArguments[0][0], false)
        }

        function test_callBoolean(data) {
            root.targetBackend = data.target
            onCallBooleanObjectFunctionSpy.clear()
            signalWithBooleanObjectSpy.clear()
            root.targetBackend.triggerCallWithBooleanObject(true)
            wait(1)
            compare(onCallBooleanObjectFunctionSpy.count, 1)
            compare(onCallBooleanObjectFunctionSpy.signalArguments[0].length, 1)
            compare(onCallBooleanObjectFunctionSpy.signalArguments[0][0], true)
            compare(signalWithBooleanObjectSpy.count, 1)
            compare(signalWithBooleanObjectSpy.signalArguments[0].length, 1)
            compare(signalWithBooleanObjectSpy.signalArguments[0][0], true)
        }

        function test_callDouble(data) {
            root.targetBackend = data.target
            onCallDoubleFunctionSpy.clear()
            signalWithDoubleSpy.clear()
            root.targetBackend.triggerCallWithDouble(.4)
            wait(1)
            compare(onCallDoubleFunctionSpy.count, 1)
            compare(onCallDoubleFunctionSpy.signalArguments[0].length, 1)
            compare(onCallDoubleFunctionSpy.signalArguments[0][0], 0.4)
            compare(signalWithDoubleSpy.count, 1)
            compare(signalWithDoubleSpy.signalArguments[0].length, 1)
            compare(signalWithDoubleSpy.signalArguments[0][0], 0.4)
        }

        function test_callDoubleObject(data) {
            root.targetBackend = data.target
            onCallDoubleObjectFunctionSpy.clear()
            signalWithDoubleObjectSpy.clear()
            root.targetBackend.triggerCallWithDoubleObject(-.3)
            wait(1)
            compare(onCallDoubleObjectFunctionSpy.count, 1)
            compare(onCallDoubleObjectFunctionSpy.signalArguments[0].length, 1)
            compare(onCallDoubleObjectFunctionSpy.signalArguments[0][0], -.3)
            compare(signalWithDoubleObjectSpy.count, 1)
            compare(signalWithDoubleObjectSpy.signalArguments[0].length, 1)
            compare(signalWithDoubleObjectSpy.signalArguments[0][0], -.3)
        }

        function test_callLong(data) {
            root.targetBackend = data.target
            onCallLongFunctionSpy.clear()
            signalWithLongSpy.clear()
            root.targetBackend.triggerCallWithLong(100000002)
            wait(1)
            compare(onCallLongFunctionSpy.count, 1)
            compare(onCallLongFunctionSpy.signalArguments[0].length, 1)
            compare(onCallLongFunctionSpy.signalArguments[0][0], 100000002)
            compare(signalWithLongSpy.count, 1)
            compare(signalWithLongSpy.signalArguments[0].length, 1)
            compare(signalWithLongSpy.signalArguments[0][0], 100000002)
        }

        function test_callLongObject(data) {
            root.targetBackend = data.target
            onCallLongObjectFunctionSpy.clear()
            signalWithLongObjectSpy.clear()
            root.targetBackend.triggerCallWithLongObject(100000002)
            wait(1)
            compare(onCallLongObjectFunctionSpy.count, 1)
            compare(onCallLongObjectFunctionSpy.signalArguments[0].length, 1)
            compare(onCallLongObjectFunctionSpy.signalArguments[0][0], 100000002)
            compare(signalWithLongObjectSpy.count, 1)
            compare(signalWithLongObjectSpy.signalArguments[0].length, 1)
            compare(signalWithLongObjectSpy.signalArguments[0][0], 100000002)
        }

        function test_callFloat(data) {
            root.targetBackend = data.target
            onCallFloatFunctionSpy.clear()
            signalWithFloatSpy.clear()
            root.targetBackend.triggerCallWithFloat(.4)
            wait(1)
            compare(onCallFloatFunctionSpy.count, 1)
            compare(onCallFloatFunctionSpy.signalArguments[0].length, 1)
            compare(onCallFloatFunctionSpy.signalArguments[0][0], 0.4)
            compare(signalWithFloatSpy.count, 1)
            compare(signalWithFloatSpy.signalArguments[0].length, 1)
            compare(signalWithFloatSpy.signalArguments[0][0], 0.4)
        }

        function test_callFloatObject(data) {
            root.targetBackend = data.target
            onCallFloatObjectFunctionSpy.clear()
            signalWithFloatObjectSpy.clear()
            root.targetBackend.triggerCallWithFloatObject(-.34)
            wait(1)
            compare(onCallFloatObjectFunctionSpy.count, 1)
            compare(onCallFloatObjectFunctionSpy.signalArguments[0].length, 1)
            compare(onCallFloatObjectFunctionSpy.signalArguments[0][0], -.34)
            compare(signalWithFloatObjectSpy.count, 1)
            compare(signalWithFloatObjectSpy.signalArguments[0].length, 1)
            compare(signalWithFloatObjectSpy.signalArguments[0][0], -.34)
        }

        function test_callChar(data) {
            root.targetBackend = data.target
            onCallCharFunctionSpy.clear()
            signalWithCharSpy.clear()
            root.targetBackend.triggerCallWithChar('q')
            wait(1)
            compare(onCallCharFunctionSpy.count, 1)
            compare(onCallCharFunctionSpy.signalArguments[0].length, 1)
            compare(onCallCharFunctionSpy.signalArguments[0][0], 'q')
            compare(signalWithCharSpy.count, 1)
            compare(signalWithCharSpy.signalArguments[0].length, 1)
            compare(signalWithCharSpy.signalArguments[0][0], 'q')
        }

        function test_callCharObject(data) {
            root.targetBackend = data.target
            onCallCharObjectFunctionSpy.clear()
            signalWithCharObjectSpy.clear()
            root.targetBackend.triggerCallWithCharObject('t')
            wait(1)
            compare(onCallCharObjectFunctionSpy.count, 1)
            compare(onCallCharObjectFunctionSpy.signalArguments[0].length, 1)
            compare(onCallCharObjectFunctionSpy.signalArguments[0][0], 't')
            compare(signalWithCharObjectSpy.count, 1)
            compare(signalWithCharObjectSpy.signalArguments[0].length, 1)
            compare(signalWithCharObjectSpy.signalArguments[0][0], 't')
        }

        function test_callWithTwoInts(data) {
            root.targetBackend = data.target
            onCallWithTwoIntsFunctionSpy.clear()
            signalWithTwoIntsSpy.clear()
            root.targetBackend.triggerCallWithTwoInts(-3, 4)
            wait(1)
            compare(onCallWithTwoIntsFunctionSpy.count, 1)
            compare(onCallWithTwoIntsFunctionSpy.signalArguments[0].length, 2)
            compare(onCallWithTwoIntsFunctionSpy.signalArguments[0][1], 4)
            compare(onCallWithTwoIntsFunctionSpy.signalArguments[0][0], -3)
            compare(signalWithTwoIntsSpy.count, 1)
            compare(signalWithTwoIntsSpy.signalArguments[0].length, 2)
            compare(signalWithTwoIntsSpy.signalArguments[0][0], -3)
            compare(signalWithTwoIntsSpy.signalArguments[0][1], 4)
        }

        function test_callWithThreeInts(data) {
            root.targetBackend = data.target
            onCallWithThreeIntsFunctionSpy.clear()
            signalWithThreeIntsSpy.clear()
            root.targetBackend.triggerCallWithThreeInts(-3, 4,5 )
            wait(1)
            compare(onCallWithThreeIntsFunctionSpy.count, 1)
            compare(onCallWithThreeIntsFunctionSpy.signalArguments[0].length, 3)
            compare(signalWithThreeIntsSpy.count, 1)
            compare(signalWithThreeIntsSpy.signalArguments[0].length, 3)
        }

        function test_callWithIntAndString(data) {
            root.targetBackend = data.target
            onCallWithIntAndStringFunctionSpy.clear()
            signalWithIntAndStringSpy.clear()
            root.targetBackend.triggerCallWithIntAndString(33333,"qt")
            wait(1)
            compare(onCallWithIntAndStringFunctionSpy.count, 1)
            compare(onCallWithIntAndStringFunctionSpy.signalArguments[0].length, 2)
            compare(onCallWithIntAndStringFunctionSpy.signalArguments[0][0], 33333)
            compare(onCallWithIntAndStringFunctionSpy.signalArguments[0][1], "qt")
            compare(signalWithIntAndStringSpy.count, 1)
            compare(signalWithIntAndStringSpy.signalArguments[0].length, 2)
            compare(signalWithIntAndStringSpy.signalArguments[0][0], 33333)
            compare(signalWithIntAndStringSpy.signalArguments[0][1], "qt")
        }

        function test_callWithBooleanAndDouble(data) {
            root.targetBackend = data.target
            onCallWithBooleanAndDoubleFunctionSpy.clear()
            signalWithBooleanAndDoubleSpy.clear()
            root.targetBackend.triggerCallWithBooleanAndDouble(true,3.14)
            wait(1)
            compare(onCallWithBooleanAndDoubleFunctionSpy.count, 1)
            compare(onCallWithBooleanAndDoubleFunctionSpy.signalArguments[0].length, 2)
            compare(onCallWithBooleanAndDoubleFunctionSpy.signalArguments[0][0], true)
            compare(onCallWithBooleanAndDoubleFunctionSpy.signalArguments[0][1], 3.14)
            compare(signalWithBooleanAndDoubleSpy.count, 1)
            compare(signalWithBooleanAndDoubleSpy.signalArguments[0].length, 2)
            compare(signalWithBooleanAndDoubleSpy.signalArguments[0][0], true)
            compare(signalWithBooleanAndDoubleSpy.signalArguments[0][1], 3.14)
        }

        function test_callWithString(data) {
            root.targetBackend = data.target
            onCallWithStringFunctionSpy.clear()
            signalWithStringSpy.clear()
            root.targetBackend.triggerCallWithString("qt")
            wait(1)
            compare(onCallWithStringFunctionSpy.count, 1)
            compare(onCallWithStringFunctionSpy.signalArguments[0].length, 1)
            compare(onCallWithStringFunctionSpy.signalArguments[0][0], "qt")
            compare(signalWithStringSpy.count, 1)
            compare(signalWithStringSpy.signalArguments[0].length, 1)
            compare(signalWithStringSpy.signalArguments[0][0], "qt")
        }

        function test_callWithList(data) {
            root.targetBackend = data.target
            onCallWithListFunctionSpy.clear()
            signalWithListSpy.clear()
            var myList = ["apple", "banana", "cherry"];
            root.targetBackend.triggerCallWithList(myList)
            wait(1)
            compare(onCallWithListFunctionSpy.count, 1)
            compare(onCallWithListFunctionSpy.signalArguments[0].length, 1)
            var listArg = onCallWithListFunctionSpy.signalArguments[0][0]
            compare(listArg[0], "apple")
            compare(listArg[1], "banana")
            compare(listArg[2], "cherry")
            compare(signalWithListSpy.count, 1)
            compare(signalWithListSpy.signalArguments[0].length, 1)
            var signalListArg = signalWithListSpy.signalArguments[0][0]
            compare(signalListArg[0], "apple")
            compare(signalListArg[1], "banana")
            compare(signalListArg[2], "cherry")
        }

        function test_callWithMap(data) {
            root.targetBackend = data.target
            onCallWithMapSpy.clear()
            signalWithMapSpy.clear()
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
            compare(signalWithMapSpy.count, 1)
            compare(signalWithMapSpy.signalArguments[0].length, 1)
            compare(signalWithMapSpy.signalArguments[0][0], myMap)
        }

        function test_callWithUrl(data) {
            root.targetBackend = data.target
            onCallWithUrlSpy.clear()
            signalWithUrlSpy.clear()
            var myUrl = "http://example.com/path"
            root.targetBackend.triggerCallWithUrl(myUrl);
            wait(1)
            compare(onCallWithUrlSpy.count, 1)
            compare(onCallWithUrlSpy.signalArguments[0].length, 1)
            // Verify that original and received signal argument match
            var urlArg = onCallWithUrlSpy.signalArguments[0][0]
            compare(urlArg, myUrl)
            compare(signalWithUrlSpy.count, 1)
            compare(signalWithUrlSpy.signalArguments[0].length, 1)
            compare(signalWithUrlSpy.signalArguments[0][0], myUrl)
        }

        function test_callWithEnum(data) {
            root.targetBackend = data.target
            onCallWithEnumSpy.clear()
            signalWithEnumSpy.clear()
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
            compare(signalWithEnumSpy.count, 1)
            compare(signalWithEnumSpy.signalArguments[0].length, 1)
            var signalEnumArg = signalWithEnumSpy.signalArguments[0][0]
            compare(signalEnumArg.name, "BLUE")
            compare(signalEnumArg.ordinal, 2)
            compare(signalEnumArg.rgb, "#0000FF")
        }

        function test_callbackProxyToString(data) {
            // Test QtSignalProxy.toString() works (does not try to emit)
            root.targetBackend = data.target
            let string = root.targetBackend.callbackProxyToString()
            verify(string.length > 0)
        }
    }
}
