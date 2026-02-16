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

    function test_a_intArray_data() {
        return [
            {tag: "a", initial: [1, 2, 3], changed: [4, 5, 6]},
            {tag: "b", initial: [4, 5 , 6], changed: [7, 8, 9]},
        ]
    }

    function test_a_intArray(data) {
        tb.intArrayEcho(data.initial)
    }

    function test_a_IntegerArray_data() {
        return [
            {tag: "a", initial: [1, 2, 3], changed: [4, 5, 6]},
            {tag: "b", initial: [4, 5 , 6], changed: [7, 8, 9]},
        ]
    }

    function test_a_IntegerArray(data) {
        tb.IntegerArrayEcho(data.initial)
    }

    function test_a_StringArray_data() {
        return [
            {tag: "a", initial: ["aa", "bb", "cc"], changed: ["dd", "ee", "ff"]},
            {tag: "b", initial: ["dd", "ee" , "ff"], changed: ["hh", "ii", "jj"]},
        ]
    }

    function test_a_StringArray(data) {
        tb.StringArrayEcho(data.initial)
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

        tb.stringValueNull();
        compare(tb.stringProp, "");

        tb.longValueNull();
        compare(tb.longProp, 0);

        tb.uriValueNull();
        compare(tb.uriProp, "");

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

        tb.stringPropertyNull();
        ignoreWarning(warning)
        compare(tb.stringProp, "");

        tb.longPropertyNull();
        ignoreWarning(warning)
        compare(tb.longProp, 0);

        tb.uriPropertyNull();
        ignoreWarning(warning)
        compare(tb.uriProp, "");

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
