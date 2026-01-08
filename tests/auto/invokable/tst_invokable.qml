/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

import QtQuick
import QtTest

import QtBridge

Item {
    TestCase {
        name: "Invokable"
        when: windowShown

        // Integer
        function test_addInteger() { compare(TestBackend.addInteger(10, 5), 15) }
        function test_addInt() { compare(TestBackend.addInt(7, 8), 15) }

        // Long
        function test_addLongObject() { compare(TestBackend.addLongObject(10000000000, 5), 10000000005) }
        function test_addLong() { compare(TestBackend.addLong(2000000000, 147483647), 2147483647) }

        // Short
        function test_addShortObject() { compare(TestBackend.addShortObject(100, 20), 120) }
        function test_addShort() { compare(TestBackend.addShort(50, -25), 25) }

        // Byte
        function test_addByteObject() { compare(TestBackend.addByteObject(10, 20), 30) }
        function test_addByte() { compare(TestBackend.addByte(100, -50), 50) }

        // Char
        function test_addCharObject() {
            compare(TestBackend.addCharObject('A', 1), 'B')
        }
        function test_addChar() {
            compare(TestBackend.addChar('a', 2), 'c')
        }

        // Boolean
        function test_andBooleanObject() {
            compare(TestBackend.andBooleanObject(true, true), true)
            compare(TestBackend.andBooleanObject(true, false), false)
            compare(TestBackend.andBooleanObject(false, false), false)
        }
        function test_andBoolean() {
            compare(TestBackend.andBoolean(true, true), true)
            compare(TestBackend.andBoolean(false, true), false)
            compare(TestBackend.andBoolean(false, false), false)
        }

        // Float
        function test_addFloatObject() { compare(TestBackend.addFloatObject(1.5, 2.5), 4.0) }
        function test_addFloat() { compare(TestBackend.addFloat(3.2, -1.2), 2.0) }

        // Double
        function test_addDoubleObject() { compare(TestBackend.addDoubleObject(1.1, 2.2), 3.3) }
        function test_addDouble() { compare(TestBackend.addDouble(5.5, 4.5), 10.0) }

        // String
        function test_concatString() {
            compare(TestBackend.concatString("Hello", "World"), "HelloWorld")
            compare(TestBackend.concatString("", "Test"), "Test")
        }

        // List<String>
        function test_concatList() {
            var list1 = ["a", "b"]
            var list2 = ["c", "d"]
            var result = TestBackend.concatList(list1, list2)
            compare(result.length, 4)
            compare(result[0], "a")
            compare(result[1], "b")
            compare(result[2], "c")
            compare(result[3], "d")
        }

        function test_nonAsciiInvokable() {
            // Use [] notation to escape JS identifier character constraints
            compare(TestBackend["nonAs€ciiöäEcho"](51), 51);
        }
    }
    TestCase {
        name: "Invokable_Overload"
        when: windowShown

        // Typed properties to control overload selection
        property int firstInt: 10
        property double secondDouble: 20.0
        property double zeroDouble: 0.0

        function test_primitives() {
            compare(OverloadTest.invokePrimitive(3, 5), 8)
            compare(OverloadTest.invokePrimitive(10, 20), 30)
            compare(OverloadTest.invokePrimitive(0, 0), 0)
            compare(OverloadTest.invokePrimitive(-5, 5), 0)
            compare(OverloadTest.invokePrimitive(2.2, 3.3), 5.5)
            compare(OverloadTest.invokePrimitive(1.5, 2.5), 4.0)
            compare(OverloadTest.invokePrimitive(5, 2.5), 7.5)
            compare(OverloadTest.invokePrimitive(2.5, 5), 7.5)
            compare(OverloadTest.invokePrimitive(true, false), false)
            compare(OverloadTest.invokePrimitive(true, true), true)
            compare(OverloadTest.invokePrimitive("Value: ", 10), "Value: 10.0")
            compare(OverloadTest.invokePrimitive("Pi: ", secondDouble), "Pi: 20.0")
            compare(OverloadTest.invokePrimitive(100, 27), 127 & 0xFF)
            compare(OverloadTest.invokePrimitive(1000, 2000), 3000)
        }

        function test_boxed() {
            compare(OverloadTest.invokeBoxed("Number: ", 55.3), "Number: 55.3")
            compare(OverloadTest.invokeBoxed("E: ", 2.718), "E: 2.718")
            // Disabled, see QTBUG-141945 (JS numbers are doubles)
            // compare(OverloadTest.invokeBoxed("Number: ", 55), "Number: 55")
            compare(OverloadTest.invokeBoxed(firstInt, 2.7), 12.7)
            compare(OverloadTest.invokeBoxed(firstInt, secondDouble),30)
            compare(OverloadTest.invokeBoxed(true, false), false)
            compare(OverloadTest.invokeBoxed(true, true), true)
            compare(OverloadTest.invokeBoxed(firstInt, zeroDouble), 10.0)
            compare(OverloadTest.invokeBoxed(-5, 7.5), 2.5)
        }

        function test_lists() {
            compare(OverloadTest.invokeList([1, 2, 3, 4]), 10)
            compare(OverloadTest.invokeList([]), 0)
            compare(OverloadTest.invokeListDouble([1.1, 2.2, 3.3]), 6.6)
            compare(OverloadTest.invokeListDouble([]), 0.0, "Empty double list")
            compare(OverloadTest.invokeListString(["a", "b", "c"]), "a, b, c")
            compare(OverloadTest.invokeListString([]), "")
            compare(OverloadTest.invokeListGeneric([true, false, true]), "List: [true, false, true]")
            compare(OverloadTest.invokeListGeneric(["x", "y", "z"]), "List: [x, y, z]")
            compare(OverloadTest.invokeListGeneric([]), "List: []")
        }

        function test_map() {
            var flatMap1 = { k1: 3, k2: 7};
            compare(OverloadTest.echoMap(flatMap1), flatMap1);
            compare(OverloadTest.sumFlatMapValues(flatMap1), 10);
            var flatMap2 = {};
            compare(OverloadTest.echoMap(flatMap2), flatMap2);
            compare(OverloadTest.sumFlatMapValues(flatMap2), 0);

            var nestedMap1 = { k1: 1, o1: { k2: 2 } } // contains object
            compare(OverloadTest.echoMap(nestedMap1), nestedMap1);

            var nestedMap2 = { k1: 1, a1: [1, 2, 3] } // contains array
            compare(OverloadTest.echoMap(nestedMap2), nestedMap2);

            var complexMap = {
                k1: 1,
                a1: [
                    { k2: 2 },
                    { k3: 3 }
                ],
                o1: {
                    k4: 4,
                    a2: [1, 2, 3]
                }
            }
            compare(OverloadTest.echoMap(complexMap), complexMap);
        }

        function test_url() {
            // Basic URL
            var url = "http://example.com/path";
            compare(OverloadTest.echoUrl(url), url);
            // Empty URL
            url = ""
            compare(OverloadTest.echoUrl(url), url);
        }
    }
}
