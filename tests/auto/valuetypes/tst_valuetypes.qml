/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

import QtQuick
import QtTest

import QtBridge

TestCase {
    id: tc
    name: "ValueTypes"
    when: windowShown

    MyType {
        id: myTypeInstance1
        intProp: 22
    }

    MyType {
        id: myTypeInstance2
        intProp: 33
    }

    MyType {
        id: myTypeInstance3
        intProp: 44
    }

    MyType {
        id: myTypeInstance4
        intProp: 55
    }

    Rectangle {
        id: rect
    }

    // Tests need a lot of spies. Introduce a small spy factory so we don't
    // need to specify all of them by hand
    Component {
        id: spyComponent
        SignalSpy {
            target: tb
            signalName: ""
        }
    }
    property var spiesByName: ({})
    // Returns spy by name. Creates (and caches) one if it didn't exist yet
    function spy(name) {
        if (!spiesByName[name])
            spiesByName[name] = spyComponent.createObject(tc, { signalName: name })
        return spiesByName[name]
    }

    function init() {
        // Clear all spies before each test case (called also
        // between data-driven case rows)
        for (var name in spiesByName) {
            var s = spiesByName[name];
            if (s && s.clear)
                s.clear();
        }
    }

    ValueTypesTestBackend {
        id: tb
    }

    // Double, double
    function test_a_double_data() {
        return [
            {tag: "positive", initial: 2.2, changed: 3.2},
            {tag: "negative1", initial: 3.2, changed: -4.2},
            {tag: "negative2", initial: -4.2, changed: 5.2}
        ]
    }
    function test_a_double(data) {
        var propChangedSpy = spy("doublePropChanged")
        var boxedCallbackSpy = spy("boxedDoubleSignal")
        var unboxedCallbackSpy = spy("unboxedDoubleSignal")
        compare(tb.doubleProp, data.initial)
        tb.doubleProp = data.changed
        wait(1); compare(propChangedSpy.count, 1)
        compare(tb.doubleProp, data.changed)
        compare(tb.boxedDoubleEcho(data.changed), data.changed)
        wait(1); compare(boxedCallbackSpy.count, 1)
        compare(boxedCallbackSpy.signalArguments[0][0], data.changed)
        compare(tb.unboxedDoubleEcho(data.changed), data.changed)
        wait(1); compare(unboxedCallbackSpy.count, 1)
        compare(unboxedCallbackSpy.signalArguments[0][0], data.changed)
    }

    // Float, float
    function test_a_float_data() {
        return [
            {tag: "positive", initial: 2.2, changed: 3.2},
            {tag: "negative1", initial: 3.2, changed: -4.2},
            {tag: "negative2", initial: -4.2, changed: 5.2}
        ]
    }
    function test_a_float(data) {
        var propChangedSpy = spy("floatPropChanged")
        var boxedCallbackSpy = spy("boxedFloatSignal")
        var unboxedCallbackSpy = spy("unboxedFloatSignal")
        compare(tb.floatProp, data.initial)
        tb.floatProp = data.changed
        wait(1); compare(propChangedSpy.count, 1)
        compare(tb.floatProp, data.changed)
        compare(tb.boxedFloatEcho(data.changed), data.changed)
        wait(1); compare(boxedCallbackSpy.count, 1)
        compare(boxedCallbackSpy.signalArguments[0][0], data.changed)
        compare(tb.unboxedFloatEcho(data.changed), data.changed)
        wait(1); compare(unboxedCallbackSpy.count, 1)
        compare(unboxedCallbackSpy.signalArguments[0][0], data.changed)
    }

    // Long, long
    function test_a_long_data() {
        return [
            {tag: "positive", initial: 2, changed: 3},
            {tag: "negative", initial: 3, changed: -3}
        ]
    }
    function test_a_long(data) {
        var propChangedSpy = spy("longPropChanged")
        var boxedCallbackSpy = spy("boxedLongSignal")
        var unboxedCallbackSpy = spy("unboxedLongSignal")
        compare(tb.longProp, data.initial)
        tb.longProp = data.changed
        wait(1); compare(propChangedSpy.count, 1)
        compare(tb.longProp, data.changed)
        compare(tb.boxedLongEcho(data.changed), data.changed)
        wait(1); compare(boxedCallbackSpy.count, 1)
        compare(boxedCallbackSpy.signalArguments[0][0], data.changed)
        compare(tb.unboxedLongEcho(data.changed), data.changed)
        wait(1); compare(unboxedCallbackSpy.count, 1)
        compare(unboxedCallbackSpy.signalArguments[0][0], data.changed)
    }

    // Integer, int
    function test_a_int_data() {
        return [
            {tag: "positive", initial: 2, changed: 3},
            {tag: "negative1", initial: 3, changed: -3},
            {tag: "negative2", initial: -3, changed: 4}
        ]
    }
    function test_a_int(data) {
        var propChangedSpy = spy("intPropChanged")
        var boxedCallbackSpy = spy("boxedIntSignal")
        var unboxedCallbackSpy = spy("unboxedIntSignal")
        compare(tb.intProp, data.initial)
        tb.intProp = data.changed
        wait(1); compare(propChangedSpy.count, 1)
        compare(tb.intProp, data.changed)
        compare(tb.boxedIntEcho(data.changed), data.changed)
        wait(1); compare(boxedCallbackSpy.count, 1)
        compare(boxedCallbackSpy.signalArguments[0][0], data.changed)
        compare(tb.unboxedIntEcho(data.changed), data.changed)
        wait(1); compare(unboxedCallbackSpy.count, 1)
        compare(unboxedCallbackSpy.signalArguments[0][0], data.changed)
    }

    // Short, short
    function test_a_short_data() {
        return [
            {tag: "positive", initial: 52, changed: 53},
            {tag: "negative1", initial: 53, changed: -53},
            {tag: "negative2", initial: -53, changed: 54}
        ]
    }
    function test_a_short(data) {
        var propChangedSpy = spy("shortPropChanged")
        var boxedCallbackSpy = spy("boxedShortSignal")
        var unboxedCallbackSpy = spy("unboxedShortSignal")
        compare(tb.shortProp, data.initial)
        tb.shortProp = data.changed
        wait(1); compare(propChangedSpy.count, 1)
        compare(tb.shortProp, data.changed)
        compare(tb.boxedShortEcho(data.changed), data.changed)
        wait(1); compare(boxedCallbackSpy.count, 1)
        compare(boxedCallbackSpy.signalArguments[0][0], data.changed)
        compare(tb.unboxedShortEcho(data.changed), data.changed)
        wait(1); compare(unboxedCallbackSpy.count, 1)
        compare(unboxedCallbackSpy.signalArguments[0][0], data.changed)
    }

    // Byte, byte
    function test_a_byte_data() {
        return [
            {tag: "positive", initial: 52, changed: 53},
            {tag: "negative1", initial: 53, changed: -53},
            {tag: "negative2", initial: -53, changed: 54}
        ]
    }
    function test_a_byte(data) {
        var propChangedSpy = spy("bytePropChanged")
        var boxedCallbackSpy = spy("boxedByteSignal")
        var unboxedCallbackSpy = spy("unboxedByteSignal")
        compare(tb.byteProp, data.initial)
        tb.byteProp = data.changed
        wait(1); compare(propChangedSpy.count, 1)
        compare(tb.byteProp, data.changed)
        compare(tb.boxedByteEcho(data.changed), data.changed)
        wait(1); compare(boxedCallbackSpy.count, 1)
        compare(boxedCallbackSpy.signalArguments[0][0], data.changed)
        compare(tb.unboxedByteEcho(data.changed), data.changed)
        wait(1); compare(unboxedCallbackSpy.count, 1)
        compare(unboxedCallbackSpy.signalArguments[0][0], data.changed)
    }

    // Character, char
    function test_a_char_data() {
        return [
            {tag: "ascii", initial: 'a', changed: 'b'},
            {tag: "utf1", initial: 'b', changed: '€'},
            {tag: "utf2", initial: '€', changed: '¥'}
        ]
    }
    function test_a_char(data) {
        var propChangedSpy = spy("charPropChanged")
        var boxedCallbackSpy = spy("boxedCharSignal")
        var unboxedCallbackSpy = spy("unboxedCharSignal")
        compare(tb.charProp, data.initial)
        tb.charProp = data.changed
        wait(1);
        compare(propChangedSpy.count, 1)
        compare(tb.charProp, data.changed)
        compare(tb.boxedCharEcho(data.changed), data.changed)
        wait(1);
        compare(boxedCallbackSpy.count, 1)
        compare(boxedCallbackSpy.signalArguments[0][0], data.changed)
        compare(tb.unboxedCharEcho(data.changed), data.changed)
        wait(1);
        compare(unboxedCallbackSpy.count, 1)
        compare(unboxedCallbackSpy.signalArguments[0][0], data.changed)
    }

    // String
    function test_a_string_data() {
        return [
            {tag: "ascii", initial: "aa", changed: "bb"},
            {tag: "empty", initial: "bb", changed: ""},
            {tag: "utf1", initial: "", changed: "€€"},
            {tag: "utf2", initial: "€€", changed: "¥¥"}
        ]
    }
    function test_a_string(data) {
        var propChangedSpy = spy("stringPropChanged")
        var callbackSpy = spy("stringSignal")
        compare(tb.stringProp, data.initial)
        tb.stringProp = data.changed
        wait(1);
        compare(propChangedSpy.count, 1)
        compare(tb.stringProp, data.changed)
        compare(tb.stringEcho(data.changed), data.changed)
        wait(1);
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.changed)
    }

    // URI
    function test_a_uri_data() {
        return [
            {tag: "simple", initial: "http://x.a/a", changed: "http://x.b/b"},
            {tag: "empty1", initial:  "http://x.b/b", changed: ""},
            {tag: "empty2", initial:  "", changed: "http://x.b/e"},
        ]
    }
    function test_a_uri(data) {
        var propChangedSpy = spy("uriPropChanged")
        var callbackSpy = spy("uriSignal")
        compare(tb.uriProp, data.initial)
        tb.uriProp = data.changed
        wait(1);
        compare(propChangedSpy.count, 1)
        compare(tb.uriProp, data.changed)
        compare(tb.uriEcho(data.changed), data.changed)
        wait(1);
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.changed)
    }

    // Map<String, Object>
    function test_a_map_data() {
        var map1 =  { k1:1, k2:2 }
        var map2 = { k3:3, k4: 4, k5:5 }
        var map3 = { k6:6 }
        var map4 =
            {
                k1: 5, k2: "five", k3: "3.141",  k4: true, k5: false,
                a1: [ 1, 2, 3 ],
                o1: {
                    ok1: 7,
                    oa1: [
                        { oak1: 8, oak2: "eight" },
                        { oak1: 9, oak2: "nine" }
                    ]
                }
            }

        var map5 = {
            k1: 1,
            k2: null,
            a1: [ 8, 9, null]
        }

        var map6 = {
            k1: 21,
            k2: myTypeInstance1,
            a1: [ 6, 7, myTypeInstance2 ],
            k3: myTypeInstance3
        }

        var map7 = {
            k1: "foo",
            k2: myTypeInstance2,
            o1: { k1: "bar", k2: myTypeInstance3},
            k3: myTypeInstance4
        }

        return [
            { tag: "flat1", initial: map1, changed: map2 },
            { tag: "flat2", initial: map2, changed: map3 },
            { tag: "empty", initial: map3, changed: {} },
            { tag: "complex1", initial: {}, changed: map4 },
            { tag: "complex2", initial: map4, changed: map5 },
            { tag: "complex3", initial: map5, changed: map6 },
            { tag: "complex4", initial: map6, changed: map7 },
        ]
    }
    function test_a_map(data) {
        var propChangedSpy = spy("mapPropChanged")
        var callbackSpy = spy("mapSignal")
        compare(tb.mapProp, data.initial)
        tb.mapProp = data.changed
        wait(1);
        compare(propChangedSpy.count, 1)
        compare(tb.mapProp, data.changed)
        compare(tb.mapEcho(data.changed), data.changed)
        wait(1);
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.changed)
    }

    // List<Object>
    function test_a_list_data() {
        var slist1 = ["aa", "bb"]
        var slist2 = ["aa", "bb", "cc"]
        var ilist1 = [1, 2]
        var ilist2 = [2, 3, 4]
        var dlist1 = [1.1, 2.2]
        var dlist2 = [2.2, 3.3, 4.4]
        var mixed1 = [6.7, 8, "foo", true]
        var mixed2 = ["bar", "baz", false, 52]
        var olist1 = [myTypeInstance1, myTypeInstance2]
        var olist2 = [myTypeInstance3, myTypeInstance4]

        return [
            { tag: "string", initial: slist1, changed: slist2 },
            { tag: "empty", initial: slist2, changed: [] },
            { tag: "int1", initial: [], changed: ilist1 },
            { tag: "int2", initial: ilist1, changed: ilist2 },
            { tag: "double1", initial: ilist2, changed: dlist1 },
            { tag: "double2", initial: dlist1, changed: dlist2 },
            { tag: "mixed1", initial: dlist2, changed: mixed1 },
            { tag: "mixed2", initial: mixed1, changed: mixed2 },
            { tag: "olist1", initial: mixed2, changed: olist1 },
            { tag: "olist2", initial: olist1, changed: olist2 },
        ]
    }
    function test_a_list(data) {
        var propChangedSpy = spy("listPropChanged")
        var callbackSpy = spy("listSignal")
        compare(tb.listProp, data.initial)
        tb.listProp = data.changed
        wait(1);
        compare(propChangedSpy.count, 1)
        compare(tb.listProp, data.changed)
        compare(tb.listEcho(data.changed), data.changed)
        wait(1);
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.changed)
    }

    // List<List<...>>
    function test_a_listOfLists_data() {
        return [
            { tag: "int", initial: [[1, 2], [3, 4]], changed: [[5, 6], [7, 8, 9]] },
            { tag: "String", initial: [["aa", "bb"], ["cc"]], changed: [["€€"], ["dd", "ee"]] },
        ]
    }
    function test_a_listOfLists(data) {
        var propChangedSpy = spy("listPropChanged")
        var callbackSpy = spy("listSignal")

        tb.listProp = data.initial
        wait(1)
        propChangedSpy.clear()
        callbackSpy.clear()

        compare(tb.listProp, data.initial)
        tb.listProp = data.changed
        wait(1)
        compare(propChangedSpy.count, 1)
        compare(tb.listProp, data.changed)
        compare(tb.listEcho(data.changed), data.changed)
        wait(1)
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.changed)
    }

    // List<String>
    function test_a_stringlist_data() {
        var list1 = ["aa", "bb"]
        var list2 = ["aa", "bb", "cc"]
        var list3 = ["¥¥", "€€"]

        return [
            { tag: "ascii1", initial: list1, changed: list2 },
            { tag: "utf", initial: list2, changed: list3 },
            { tag: "empty", initial: list3, changed: [] },
            { tag: "ascii2", initial: [], changed: list2 },
        ]
    }
    function test_a_stringlist(data) {
        var propChangedSpy = spy("stringListPropChanged")
        var callbackSpy = spy("stringListSignal")
        compare(tb.stringListProp, data.initial)
        tb.stringListProp = data.changed
        wait(1);
        compare(propChangedSpy.count, 1)
        compare(tb.stringListProp, data.changed)
        compare(tb.stringListEcho(data.changed), data.changed)
        wait(1);
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.changed)
    }

    // Registrable
    function test_a_registrable_data() {
        return [
            { initialValue: 11, newRegistrable: myTypeInstance1, changedValue: 22 },
            { initialValue: 22, newRegistrable: myTypeInstance2, changedValue: 33 },
        ]
    }
    function test_a_registrable(data) {
        var propChangedSpy = spy("registrablePropChanged")
        var callbackSpy = spy("registrableSignal")
        compare(tb.registrableProp.intProp, data.initialValue)
        tb.registrableProp = data.newRegistrable
        wait(1);
        compare(propChangedSpy.count, 1)
        compare(tb.registrableProp, data.newRegistrable)
        compare(tb.registrableProp.intProp, data.changedValue)
        var echoed = tb.registrableEcho(data.newRegistrable)
        compare(echoed, data.newRegistrable)
        compare(echoed.intProp, data.changedValue)
        wait(1);
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.newRegistrable)
        compare(callbackSpy.signalArguments[0][0].intProp, data.changedValue)
    }

    // Boolean, bool
    function test_a_bool_data() {
        return [
            {tag: "first", initial: true, changed: false},
            {tag: "second", initial: false, changed: true},
        ]
    }
    function test_a_bool(data) {
        var propChangedSpy = spy("boolPropChanged")
        var boxedCallbackSpy = spy("boxedBoolSignal")
        var unboxedCallbackSpy = spy("unboxedBoolSignal")
        compare(tb.boolProp, data.initial)
        tb.boolProp = data.changed
        wait(1);
        compare(propChangedSpy.count, 1)
        compare(tb.boolProp, data.changed)
        compare(tb.boxedBoolEcho(data.changed), data.changed)
        wait(1);
        compare(boxedCallbackSpy.count, 1)
        compare(boxedCallbackSpy.signalArguments[0][0], data.changed)
        compare(tb.unboxedBoolEcho(data.changed), data.changed)
        wait(1);
        compare(unboxedCallbackSpy.count, 1)
        compare(unboxedCallbackSpy.signalArguments[0][0], data.changed)
    }

    function test_a_enum() {
        var initialColor = tb.enumProp
        compare(tb.enumProp.name, "RED");
        compare(tb.enumProp.ordinal, 0);
        compare(tb.enumProp.rgb, "#FF0000");
        tb.enumChange();
        compare(tb.enumProp.name, "BLUE");
        compare(tb.enumProp.ordinal, 2);
        compare(tb.enumProp.rgb, "#0000FF");

        // Enums are read-only properties (QTBUG-141710), verify that
        // we can't write to them
        var exception = ""
        try {
            tb.enumProp = initialColor;
        } catch (error) {
            exception = error.message
        }
        verify(exception.indexOf("Cannot assign to read-only property") !== -1)
        // Value hasn't changed:
        compare(tb.enumProp.name, "BLUE");
        compare(tb.enumProp.ordinal, 2);
        compare(tb.enumProp.rgb, "#0000FF");
    }

    function test_a_integerArray_data() {
        return [
            {property: "intArrayProp", echo: "intArrayEcho", signal: "intArraySignal", initial: [1, 2, 3], changed: [4, 5, 6]},
            {property: "intArrayProp", echo: "intArrayEcho", signal: "intArraySignal", initial: [4, 5, 6], changed: [7, 8, 9]},
            {property: "integerArrayProp", echo: "IntegerArrayEcho", signal: "IntegerArraySignal", initial: [1, 2, 3], changed: [4, 5, 6]},
            {property: "integerArrayProp", echo: "IntegerArrayEcho", signal: "IntegerArraySignal", initial: [4, 5, 6], changed: [7, 8, 9]},
        ]
    }

    function test_a_integerArray(data) {
        var callbackSpy = spy(data.signal)
        compare(tb[data.property], data.initial)
        compare(tb[data.echo](data.initial), data.initial)
        wait(1)
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.initial)
        tb[data.property] = data.changed
        wait(1)
        compare(tb[data.property], data.changed)
        compare(tb[data.echo](data.changed), data.changed)
        wait(1)
        compare(callbackSpy.count, 2)
        compare(callbackSpy.signalArguments[1][0], data.changed)
    }

    function test_a_StringArray_data() {
        return [
            {property: "stringArrayProp", echo: "StringArrayEcho", signal: "StringArraySignal", initial: ["aa", "bb", "cc"], changed: ["dd", "€€", "ff"]},
            {property: "stringArrayProp", echo: "StringArrayEcho", signal: "StringArraySignal", initial: ["dd", "€€", "ff"], changed: ["hh", "ii", "jj"]},
        ]
    }

    function test_a_StringArray(data) {
        var callbackSpy = spy(data.signal)
        compare(tb[data.property], data.initial)
        compare(tb[data.echo](data.initial), data.initial)
        wait(1)
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.initial)
        tb[data.property] = data.changed
        wait(1)
        compare(tb[data.property], data.changed)
        compare(tb[data.echo](data.changed), data.changed)
        wait(1)
        compare(callbackSpy.count, 2)
        compare(callbackSpy.signalArguments[1][0], data.changed)
    }

    function test_a_doubleArray_data() {
        return [
            {property: "doubleArrayProp", echo: "doubleArrayEcho", signal: "doubleArraySignal", initial: [1.1, 2.2, 3.3], changed: [4.4, 5.5, 6.6]},
            {property: "doubleArrayProp", echo: "doubleArrayEcho", signal: "doubleArraySignal", initial: [4.4, 5.5, 6.6], changed: [7.7, 8.8, 9.9]},
            {property: "boxedDoubleArrayProp", echo: "DoubleArrayEcho", signal: "DoubleArraySignal", initial: [1.1, 2.2, 3.3], changed: [4.4, 5.5, 6.6]},
            {property: "boxedDoubleArrayProp", echo: "DoubleArrayEcho", signal: "DoubleArraySignal", initial: [4.4, 5.5, 6.6], changed: [7.7, 8.8, 9.9]},
        ]
    }

    function test_a_doubleArray(data) {
        var callbackSpy = spy(data.signal)
        compare(tb[data.property], data.initial)
        compare(tb[data.echo](data.initial), data.initial)
        wait(1)
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.initial)
        tb[data.property] = data.changed
        wait(1)
        compare(tb[data.property], data.changed)
        compare(tb[data.echo](data.changed), data.changed)
        wait(1)
        compare(callbackSpy.count, 2)
        compare(callbackSpy.signalArguments[1][0], data.changed)
    }

    function test_a_floatArray_data() {
        return [
            {property: "floatArrayProp", echo: "floatArrayEcho", signal: "floatArraySignal", initial: [1.5, 2.5, 3.5], changed: [4.5, 5.5, 6.5]},
            {property: "floatArrayProp", echo: "floatArrayEcho", signal: "floatArraySignal", initial: [4.5, 5.5, 6.5], changed: [7.5, 8.5, 9.5]},
            {property: "boxedFloatArrayProp", echo: "FloatArrayEcho", signal: "FloatArraySignal", initial: [1.5, 2.5, 3.5], changed: [4.5, 5.5, 6.5]},
            {property: "boxedFloatArrayProp", echo: "FloatArrayEcho", signal: "FloatArraySignal", initial: [4.5, 5.5, 6.5], changed: [7.5, 8.5, 9.5]},
        ]
    }

    function test_a_floatArray(data) {
        var callbackSpy = spy(data.signal)
        compare(tb[data.property], data.initial)
        compare(tb[data.echo](data.initial), data.initial)
        wait(1)
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.initial)
        tb[data.property] = data.changed
        wait(1)
        compare(tb[data.property], data.changed)
        compare(tb[data.echo](data.changed), data.changed)
        wait(1)
        compare(callbackSpy.count, 2)
        compare(callbackSpy.signalArguments[1][0], data.changed)
    }

    function test_a_longArray_data() {
        return [
            {property: "longArrayProp", echo: "longArrayEcho", signal: "longArraySignal", initial: [1001, 1002, 1003], changed: [2001, 2002, 2003]},
            {property: "longArrayProp", echo: "longArrayEcho", signal: "longArraySignal", initial: [2001, 2002, 2003], changed: [3001, 3002, 3003]},
            {property: "boxedLongArrayProp", echo: "LongArrayEcho", signal: "LongArraySignal", initial: [1001, 1002, 1003], changed: [2001, 2002, 2003]},
            {property: "boxedLongArrayProp", echo: "LongArrayEcho", signal: "LongArraySignal", initial: [2001, 2002, 2003], changed: [3001, 3002, 3003]},
        ]
    }

    function test_a_longArray(data) {
        var callbackSpy = spy(data.signal)
        compare(tb[data.property], data.initial)
        compare(tb[data.echo](data.initial), data.initial)
        wait(1)
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.initial)
        tb[data.property] = data.changed
        wait(1)
        compare(tb[data.property], data.changed)
        compare(tb[data.echo](data.changed), data.changed)
        wait(1)
        compare(callbackSpy.count, 2)
        compare(callbackSpy.signalArguments[1][0], data.changed)
    }

    function test_a_shortArray_data() {
        return [
            {property: "shortArrayProp", echo: "shortArrayEcho", signal: "shortArraySignal", initial: [11, 22, 33], changed: [44, 55, 66]},
            {property: "shortArrayProp", echo: "shortArrayEcho", signal: "shortArraySignal", initial: [44, 55, 66], changed: [77, 88, 99]},
            {property: "boxedShortArrayProp", echo: "ShortArrayEcho", signal: "ShortArraySignal", initial: [11, 22, 33], changed: [44, 55, 66]},
            {property: "boxedShortArrayProp", echo: "ShortArrayEcho", signal: "ShortArraySignal", initial: [44, 55, 66], changed: [77, 88, 99]},
        ]
    }

    function test_a_shortArray(data) {
        var callbackSpy = spy(data.signal)
        compare(tb[data.property], data.initial)
        compare(tb[data.echo](data.initial), data.initial)
        wait(1)
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.initial)
        tb[data.property] = data.changed
        wait(1)
        compare(tb[data.property], data.changed)
        compare(tb[data.echo](data.changed), data.changed)
        wait(1)
        compare(callbackSpy.count, 2)
        compare(callbackSpy.signalArguments[1][0], data.changed)
    }

    function test_a_charArray_data() {
        return [
            {property: "charArrayProp", echo: "charArrayEcho", signal: "charArraySignal", initial: ['a', 'b', 'c'], changed: ['d', '€', 'f']},
            {property: "charArrayProp", echo: "charArrayEcho", signal: "charArraySignal", initial: ['d', '€', 'f'], changed: ['g', 'h', 'i']},
            {property: "boxedCharArrayProp", echo: "CharacterArrayEcho", signal: "CharacterArraySignal", initial: ['a', 'b', 'c'], changed: ['d', '€', 'f']},
            {property: "boxedCharArrayProp", echo: "CharacterArrayEcho", signal: "CharacterArraySignal", initial: ['d', '€', 'f'], changed: ['g', 'h', 'i']},
        ]
    }

    function test_a_charArray(data) {
        var callbackSpy = spy(data.signal)
        compare(tb[data.property], data.initial)
        compare(tb[data.echo](data.initial), data.initial)
        wait(1)
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.initial)
        tb[data.property] = data.changed
        wait(1)
        compare(tb[data.property], data.changed)
        compare(tb[data.echo](data.changed), data.changed)
        wait(1)
        compare(callbackSpy.count, 2)
        compare(callbackSpy.signalArguments[1][0], data.changed)
    }

    function test_a_byteArray_data() {
        return [
            {property: "byteArrayProp", echo: "byteArrayEcho", signal: "byteArraySignal", initial: [1, 2, 3], changed: [4, 5, 6]},
            {property: "byteArrayProp", echo: "byteArrayEcho", signal: "byteArraySignal", initial: [4, 5, 6], changed: [7, 8, 9]},
            {property: "boxedByteArrayProp", echo: "ByteArrayEcho", signal: "ByteArraySignal", initial: [1, 2, 3], changed: [4, 5, 6]},
            {property: "boxedByteArrayProp", echo: "ByteArrayEcho", signal: "ByteArraySignal", initial: [4, 5, 6], changed: [7, 8, 9]},
        ]
    }

    function test_a_byteArray(data) {
        var callbackSpy = spy(data.signal)
        compare(tb[data.property], data.initial)
        compare(tb[data.echo](data.initial), data.initial)
        wait(1)
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.initial)
        tb[data.property] = data.changed
        wait(1)
        compare(tb[data.property], data.changed)
        compare(tb[data.echo](data.changed), data.changed)
        wait(1)
        compare(callbackSpy.count, 2)
        compare(callbackSpy.signalArguments[1][0], data.changed)
    }

    function test_a_booleanArray_data() {
        return [
            {property: "booleanArrayProp", echo: "booleanArrayEcho", signal: "booleanArraySignal", initial: [true, false, true], changed: [false, true, false]},
            {property: "booleanArrayProp", echo: "booleanArrayEcho", signal: "booleanArraySignal", initial: [false, true, false], changed: [true, true, false]},
            {property: "boxedBooleanArrayProp", echo: "BooleanArrayEcho", signal: "BooleanArraySignal", initial: [true, false, true], changed: [false, true, false]},
            {property: "boxedBooleanArrayProp", echo: "BooleanArrayEcho", signal: "BooleanArraySignal", initial: [false, true, false], changed: [true, true, false]},
        ]
    }

    function test_a_booleanArray(data) {
        var callbackSpy = spy(data.signal)
        compare(tb[data.property], data.initial)
        compare(tb[data.echo](data.initial), data.initial)
        wait(1)
        compare(callbackSpy.count, 1)
        compare(callbackSpy.signalArguments[0][0], data.initial)
        tb[data.property] = data.changed
        wait(1)
        compare(tb[data.property], data.changed)
        compare(tb[data.echo](data.changed), data.changed)
        wait(1)
        compare(callbackSpy.count, 2)
        compare(callbackSpy.signalArguments[1][0], data.changed)
    }

    function test_b_javaside_valueset_data() {
        return [
            {tag: "int", property: "intProp", setter: "setIntProperty", value: 123},
            {tag: "long", property: "longProp", setter: "setLongProperty", value: 456},
            {tag: "double", property: "doubleProp", setter: "setDoubleProperty", value: 1.23},
            {tag: "float", property: "floatProp", setter: "setFloatProperty", value: 4.56},
            {tag: "char", property: "charProp", setter: "setCharProperty", value: 'c'},
            {tag: "bool", property: "boolProp", setter: "setBoolProperty", value: false},
            {tag: "short", property: "shortProp", setter: "setShortProperty", value: 159},
            {tag: "byte", property: "byteProp", setter: "setByteProperty", value: 81},
            {tag: "string", property: "stringProp", setter: "setStringProperty", value: "foo"},
            {tag: "intArray", property: "intArrayProp", setter: "setIntArrayProperty", value: [11, 22, 33]},
            {tag: "integerArray", property: "integerArrayProp", setter: "setIntegerArrayProperty", value: [44, 55, 66]},
            {tag: "doubleArray", property: "doubleArrayProp", setter: "setDoubleArrayProperty", value: [1.25, 2.5, 3.75]},
            {tag: "boxedDoubleArray", property: "boxedDoubleArrayProp", setter: "setBoxedDoubleArrayProperty", value: [4.25, 5.5, 6.75]},
            {tag: "floatArray", property: "floatArrayProp", setter: "setFloatArrayProperty", value: [1.5, 2.5, 3.5]},
            {tag: "boxedFloatArray", property: "boxedFloatArrayProp", setter: "setBoxedFloatArrayProperty", value: [4.5, 5.5, 6.5]},
            {tag: "longArray", property: "longArrayProp", setter: "setLongArrayProperty", value: [2001, 2002, 2003]},
            {tag: "boxedLongArray", property: "boxedLongArrayProp", setter: "setBoxedLongArrayProperty", value: [4001, 4002, 4003]},
            {tag: "shortArray", property: "shortArrayProp", setter: "setShortArrayProperty", value: [21, 22, 23]},
            {tag: "boxedShortArray", property: "boxedShortArrayProp", setter: "setBoxedShortArrayProperty", value: [31, 32, 33]},
            {tag: "charArray", property: "charArrayProp", setter: "setCharArrayProperty", value: ['x', 'y', 'z']},
            {tag: "boxedCharArray", property: "boxedCharArrayProp", setter: "setBoxedCharArrayProperty", value: ['q', 'w', '€']},
            {tag: "byteArray", property: "byteArrayProp", setter: "setByteArrayProperty", value: [9, 10, 11]},
            {tag: "boxedByteArray", property: "boxedByteArrayProp", setter: "setBoxedByteArrayProperty", value: [12, 13, 14]},
            {tag: "booleanArray", property: "booleanArrayProp", setter: "setBooleanArrayProperty", value: [true, false, true]},
            {tag: "boxedBooleanArray", property: "boxedBooleanArrayProp", setter: "setBoxedBooleanArrayProperty", value: [false, true, false]},
            {tag: "stringArray", property: "stringArrayProp", setter: "setStringArrayProperty", value: ["foo", "bar", "€€"]},
            {tag: "uri", property: "uriProp", setter: "setUriProperty", value: "http://www.example.com/path"},
            {tag: "registrable", property: "registrableProp", setter: "setRegistrableProperty", value: myTypeInstance4},
            {tag: "map", property: "mapProp", setter: "setMapProperty", value: { k1: "foo", k2: 123 }},
            {tag: "list", property: "listProp", setter: "setListProperty", value: [ 1, "foo", 2, null]},
            {tag: "stringList", property: "stringListProp", setter: "setStringListProperty", value: ["aa", "bb"]},
            {tag: "enum", property: "enumProp", setter: "setEnumProperty", value: "RED"},
        ]
    }

    // Test case where QtProperty is set on the Java-side
    function test_b_javaside_valueset(data) {
        var propChangedSpy = spy(data.property + "Changed");
        tb[data.setter](data.value);
        wait(1);
        compare(propChangedSpy.count, 1)
        if (data.property === "enumProp")
            compare(tb[data.property].name, data.value);
        else
            compare(tb[data.property], data.value);
    }

    function test_c_null_value() {
        // Tests that we deal with null property values gracefully
        tb.shortValueNull();
        compare(tb.shortProp, 0);

        tb.byteValueNull();
        compare(tb.byteProp, 0);

        tb.charValueNull();
        compare(tb.charProp, '\0');

        tb.boolValueNull();
        compare(tb.boolProp, false);

        tb.floatValueNull();
        compare(tb.floatProp, 0);

        tb.doubleValueNull();
        compare(tb.doubleProp, 0);

        tb.intValueNull();
        compare(tb.intProp, 0);

        tb.intArrayValueNull();
        compare(tb.intArrayProp, []);

        tb.integerArrayValueNull();
        compare(tb.integerArrayProp, []);

        tb.stringValueNull();
        compare(tb.stringProp, "");

        tb.stringArrayValueNull();
        compare(tb.stringArrayProp, []);

        tb.longValueNull();
        compare(tb.longProp, 0);

        tb.longArrayValueNull();
        compare(tb.longArrayProp, []);

        tb.boxedLongArrayValueNull();
        compare(tb.boxedLongArrayProp, []);

        tb.uriValueNull();
        compare(tb.uriProp, "");

        tb.doubleArrayValueNull();
        compare(tb.doubleArrayProp, []);

        tb.boxedDoubleArrayValueNull();
        compare(tb.boxedDoubleArrayProp, []);

        tb.floatArrayValueNull();
        compare(tb.floatArrayProp, []);

        tb.boxedFloatArrayValueNull();
        compare(tb.boxedFloatArrayProp, []);

        tb.shortArrayValueNull();
        compare(tb.shortArrayProp, []);

        tb.boxedShortArrayValueNull();
        compare(tb.boxedShortArrayProp, []);

        tb.byteArrayValueNull();
        compare(tb.byteArrayProp, []);

        tb.boxedByteArrayValueNull();
        compare(tb.boxedByteArrayProp, []);

        tb.charArrayValueNull();
        compare(tb.charArrayProp, []);

        tb.boxedCharArrayValueNull();
        compare(tb.boxedCharArrayProp, []);

        tb.booleanArrayValueNull();
        compare(tb.booleanArrayProp, []);

        tb.boxedBooleanArrayValueNull();
        compare(tb.boxedBooleanArrayProp, []);

        tb.registrableValueNull();
        compare(tb.registrableProp, null);

        tb.enumValueNull();
        compare(tb.enumProp, {});

        tb.listValueNull();
        compare(tb.listProp, []);

        tb.stringListValueNull();
        compare(tb.stringListProp, []);

        tb.mapValueNull();
        compare(tb.mapProp, {});
    }

    function test_d_null_property() {
        var warning = new RegExp("Property read failed, field*")
        // Tests that we deal with null properties gracefully (QtProperty itself becomes null).
        tb.shortPropertyNull();
        ignoreWarning(warning)
        compare(tb.shortProp, 0);

        tb.bytePropertyNull();
        ignoreWarning(warning)
        compare(tb.byteProp, 0);

        tb.charPropertyNull();
        ignoreWarning(warning)
        compare(tb.charProp, '\0');

        tb.boolPropertyNull();
        ignoreWarning(warning)
        compare(tb.boolProp, false);

        tb.floatPropertyNull();
        ignoreWarning(warning)
        compare(tb.floatProp, 0);

        tb.doublePropertyNull();
        ignoreWarning(warning)
        compare(tb.doubleProp, 0);

        tb.intPropertyNull();
        ignoreWarning(warning)
        compare(tb.intProp, 0);

        tb.intArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.intArrayProp, []);

        tb.integerArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.integerArrayProp, []);

        tb.stringPropertyNull();
        ignoreWarning(warning)
        compare(tb.stringProp, "");

        tb.stringArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.stringArrayProp, []);

        tb.longPropertyNull();
        ignoreWarning(warning)
        compare(tb.longProp, 0);

        tb.longArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.longArrayProp, []);

        tb.boxedLongArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.boxedLongArrayProp, []);

        tb.uriPropertyNull();
        ignoreWarning(warning)
        compare(tb.uriProp, "");

        tb.doubleArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.doubleArrayProp, []);

        tb.boxedDoubleArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.boxedDoubleArrayProp, []);

        tb.floatArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.floatArrayProp, []);

        tb.boxedFloatArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.boxedFloatArrayProp, []);

        tb.shortArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.shortArrayProp, []);

        tb.boxedShortArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.boxedShortArrayProp, []);

        tb.byteArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.byteArrayProp, []);

        tb.boxedByteArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.boxedByteArrayProp, []);

        tb.charArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.charArrayProp, []);

        tb.boxedCharArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.boxedCharArrayProp, []);

        tb.booleanArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.booleanArrayProp, []);

        tb.boxedBooleanArrayPropertyNull();
        ignoreWarning(warning)
        compare(tb.boxedBooleanArrayProp, []);

        tb.registrablePropertyNull();
        ignoreWarning(warning)
        compare(tb.registrableProp, null);

        tb.enumPropertyNull();
        ignoreWarning(warning)
        compare(tb.enumProp, {});

        tb.listPropertyNull();
        ignoreWarning(warning)
        compare(tb.listProp, []);

        tb.stringListPropertyNull();
        ignoreWarning(warning)
        compare(tb.stringListProp, []);

        tb.mapPropertyNull();
        ignoreWarning(warning)
        compare(tb.mapProp, {});
    }
}
