/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

import QtQuick
import QtTest

import QtBridge

TestCase {
    id: tc

    name: "QtProperty"
    when: windowShown

    SignalSpy {
        id: integerValueChangedSpy
        signalName: "onIntegerPropertyValueChanged"
        target: TestBackend
    }
    SignalSpy {
        id: stringListValueChangedSpy
        signalName: "onStringListPropertyValueChanged"
        target: TestBackend
    }
    SignalSpy {
        id: mapValueChangedSpy
        signalName: "onMapPropertyValueChanged"
        target: TestBackend
    }
    SignalSpy {
        id: listValueChangedSpy
        signalName: "onListPropertyValueChanged"
        target: TestBackend
    }
    SignalSpy {
        id: urlValueChangedSpy
        signalName: "onUrlPropertyValueChanged"
        target: TestBackend
    }
    SignalSpy {
        id: enumValueChangedSpy
        signalName: "onEnumPropertyValueChanged"
        target: TestBackend
    }
    SignalSpy {
        id: nonAsciiValueChangedSpy
        signalName: "onNonAs€iiäöPropertyValueChanged"
        target: TestBackend
    }

    // Bind backend properties to QML properties to make sure
    // property bindings work
    property var integerProperty: TestBackend.integerProperty
    property var stringListProperty: TestBackend.stringListProperty
    property var mapProperty: TestBackend.mapProperty
    property var urlProperty: TestBackend.urlProperty
    property var enumProperty: TestBackend.enumProperty

    function cleanup() {
        TestBackend.stopObserving();
    }
    function init() {
        integerValueChangedSpy.clear();
        stringListValueChangedSpy.clear();
        mapValueChangedSpy.clear();
        listValueChangedSpy.clear();
        urlValueChangedSpy.clear();
        enumValueChangedSpy.clear();
        nonAsciiValueChangedSpy.clear();
        TestBackend.startObserving();
    }
    function test_a_initialValue() {
        // Integer Expected initial value
        compare(TestBackend.integerProperty, 42);
        // Bound value match
        compare(TestBackend.integerProperty, tc.integerProperty);
        // Java-side and QML-side match
        compare(TestBackend.integerPropertyValue(), TestBackend.integerProperty);

        // List<String> Expected initial value
        compare(TestBackend.stringListProperty.length, 5);
        compare(TestBackend.stringListProperty[0], "Zero");
        compare(TestBackend.stringListProperty[1], "One");
        compare(TestBackend.stringListProperty[2], "Two");
        compare(TestBackend.stringListProperty[3], "Three");
        compare(TestBackend.stringListProperty[4], "Four");
        // Bound value match
        compare(TestBackend.stringListProperty, tc.stringListProperty);
        // Java-side and QML-side match
        compare(TestBackend.stringListPropertyValue(), TestBackend.stringListProperty);

        // Map<> Expected initial value
        compare(Object.keys(TestBackend.mapProperty).length, 2);
        compare(TestBackend.mapProperty["key1"], "value1");
        compare(TestBackend.mapProperty["key2"], "value2");
        // Bound value match
        compare(TestBackend.mapProperty, tc.mapProperty);
        // Java-side and QML-side match
        compare(TestBackend.mapPropertyValue(), TestBackend.mapProperty)

        // URI Expected initial value
        var url = "http://example.com/path";
        compare(TestBackend.urlProperty, url);
        // Bound value match
        compare(TestBackend.urlProperty, tc.urlProperty);
        // Java-side and QML-side match
        compare(TestBackend.urlPropertyValue(), TestBackend.urlProperty);

        // Enum (Color) Expected initial value
        compare(TestBackend.enumProperty.name, "RED");
        compare(TestBackend.enumProperty.ordinal, 0);
        compare(TestBackend.enumProperty.rgb, "#FF0000");
        // Bound value match
        compare(TestBackend.enumProperty, tc.enumProperty);
        // Java-side QML-side match
        compare(TestBackend.enumPropertyValue(), TestBackend.enumProperty);
    }

    function test_enumValue() {
        // Changing at QML-side is not supported, see QTBUG-141709
        // Change at Java-side
        TestBackend.changeEnumProperty();
        enumValueChangedSpy.wait();
        compare(enumValueChangedSpy.count, 1);
        compare(TestBackend.enumProperty.name, "GREEN");
        compare(TestBackend.enumProperty.ordinal, 1);
        compare(TestBackend.enumProperty.rgb, "#00FF00");
        compare(TestBackend.enumProperty, tc.enumProperty);
    }

    function test_urlValue() {
        // Change at QML-side
        var url = "http://some.example.com/some/path";
        TestBackend.urlProperty = url;
        urlValueChangedSpy.wait();
        compare(urlValueChangedSpy.count, 1);
        compare(TestBackend.urlProperty, url);
        compare(TestBackend.urlProperty, tc.urlProperty);
        // Change at Java-side
        TestBackend.changeUrlPropertyValue();
        url = "http://changed.example.com/path";
        urlValueChangedSpy.wait();
        compare(urlValueChangedSpy.count, 2);
        compare(TestBackend.urlProperty, url);
        compare(TestBackend.urlProperty, tc.urlProperty);
    }

    function test_integerValue() {
        // Change at QML-side
        TestBackend.integerProperty = 43;
        integerValueChangedSpy.wait();
        compare(integerValueChangedSpy.count, 1);
        compare(TestBackend.integerProperty, 43);
        compare(TestBackend.integerPropertyValue(), 43);
        compare(TestBackend.integerProperty, tc.integerProperty);
        // Change at Java-side
        TestBackend.incrementIntegerProperty();
        integerValueChangedSpy.wait();
        compare(integerValueChangedSpy.count, 2);
        compare(TestBackend.integerProperty, 44);
        compare(TestBackend.integerPropertyValue(), 44);
        compare(TestBackend.integerProperty, tc.integerProperty);
        // Stop observing and verify we don't get notified
        TestBackend.stopObserving();
        integerValueChangedSpy.clear();
        TestBackend.incrementIntegerProperty();
        wait(10);
        compare(integerValueChangedSpy.count, 0);
        compare(TestBackend.integerProperty, 45);
        compare(TestBackend.integerPropertyValue(), 45);
        compare(TestBackend.integerProperty, tc.integerProperty);
    }

    function test_manualObservers() {
        // Stop convenience observers
        TestBackend.stopObserving();
        // Start manually created observers
        TestBackend.registerManualObservers();
        // Change the value
        TestBackend.integerProperty = 43;
        integerValueChangedSpy.wait();
        compare(integerValueChangedSpy.count, 2); // two observers, both emit
        compare(TestBackend.integerProperty, 43);
        compare(TestBackend.integerPropertyValue(), 43);
        compare(TestBackend.integerProperty, tc.integerProperty);
        // Stop observers and verify that notifying stops
        TestBackend.stopObserving();
        TestBackend.integerProperty = 44;
        wait(10);
        compare(integerValueChangedSpy.count, 2); // Still the same
        compare(TestBackend.integerProperty, 44);
        compare(TestBackend.integerPropertyValue(), 44);
        compare(TestBackend.integerProperty, tc.integerProperty);
    }
    function test_stringListValue() {
        // Change at Java-side
        TestBackend.changeStringListProperty();
        stringListValueChangedSpy.wait();
        compare(stringListValueChangedSpy.count, 1);
        compare(TestBackend.stringListProperty.length, 3);
        compare(TestBackend.stringListProperty[0], "AAA");
        compare(TestBackend.stringListProperty[1], "BBB");
        compare(TestBackend.stringListProperty[2], "CCC");
        compare(TestBackend.stringListProperty, tc.stringListProperty);

        // Change at QML-side
        const updatedListValue = ["0", "I", "II", "III"];
        TestBackend.stringListProperty = updatedListValue;
        stringListValueChangedSpy.wait();
        compare(stringListValueChangedSpy.count, 2);
        compare(TestBackend.stringListProperty.length, 4);
        compare(TestBackend.stringListProperty[0], "0");
        compare(TestBackend.stringListProperty[1], "I");
        compare(TestBackend.stringListProperty[2], "II");
        compare(TestBackend.stringListProperty[3], "III");
        compare(TestBackend.stringListProperty, tc.stringListProperty);
    }
    function test_mapValue() {
        // Change at Java-side
        TestBackend.changeMapProperty();
        mapValueChangedSpy.wait();
        compare(mapValueChangedSpy.count, 1);
        compare(Object.keys(TestBackend.mapProperty).length, 2);
        compare(TestBackend.mapProperty["key3"], "value3");
        compare(TestBackend.mapProperty["key4"], "value4");
        compare(TestBackend.mapProperty, tc.mapProperty);

        // Change at QML-side, first simple flat map
        TestBackend.mapProperty = { key5: "value5", key6: "value6", key7: "value7" }
        mapValueChangedSpy.wait();
        compare(mapValueChangedSpy.count, 2);
        compare(Object.keys(TestBackend.mapProperty).length, 3);
        compare(TestBackend.mapProperty["key5"], "value5");
        compare(TestBackend.mapProperty["key6"], "value6");
        compare(TestBackend.mapProperty["key7"], "value7");
        // Complex nested value
        var complexMap =
            {
                k1: 5,
                k2: "five",
                k3: "3.141",
                k4: true,
                k5: false,
                a1: [ 1, 2, 3 ],
                o1: {
                    ok1: 7,
                    oa1: [
                        { oak1: 8, oak2: "eight" },
                        { oak1: 9, oak2: "nine" }
                    ]
                }
            }
        TestBackend.mapProperty = complexMap
        mapValueChangedSpy.wait();
        compare(mapValueChangedSpy.count, 3);
        compare(TestBackend.mapProperty["k1"], 5)
        compare(TestBackend.mapProperty["k2"], "five")
        fuzzyCompare(TestBackend.mapProperty["k3"], 3.141, 0.001)
        compare(TestBackend.mapProperty["k4"], true)
        compare(TestBackend.mapProperty["k5"], false)
        compare(TestBackend.mapProperty["o1"]["ok1"], 7)
        compare(TestBackend.mapProperty["o1"]["oa1"][0]["oak2"], "eight")
        compare(TestBackend.mapProperty["o1"]["oa1"][1]["oak2"], "nine")

        // JSON Object with null value
        var mapWithNull = {
            k1: 1,
            k2: null,
            a1: [ 8, 9, null]
        }
        TestBackend.mapProperty = mapWithNull
        mapValueChangedSpy.wait()
        compare(mapValueChangedSpy.count, 4)
        compare(TestBackend.mapProperty["k1"], 1)
        compare(TestBackend.mapProperty["k2"], null)
        compare(TestBackend.mapProperty["a1"][1], 9)
        compare(TestBackend.mapProperty["a1"][2], null)
    }

    function test_json() {
        // Pass in JSON as a string, as if it would've arrived over network
        // Complex JSON object
        var json = "
        {
          \"k1\": 1,
          \"a1\": [ { \"k2\": 2 }, { \"k3\": 3 } ],
          \"o1\": { \"k4\": 4, \"a2\": [1,2,3] },
          \"k2\": \"hello\",
          \"k3\": true
        }
        ";
        TestBackend.setJsonObject(json);
        mapValueChangedSpy.wait();
        compare(mapValueChangedSpy.count, 1);
        compare(TestBackend.mapProperty["a1"][1]["k3"], 3);
        // Empty JSON object
        json = ""
        TestBackend.setJsonObject(json);
        mapValueChangedSpy.wait();
        compare(mapValueChangedSpy.count, 2);
        compare(Object.keys(TestBackend.mapProperty).length, 0);
        json = "{}"
        TestBackend.setJsonObject(json);
        mapValueChangedSpy.wait();
        compare(mapValueChangedSpy.count, 3);
        compare(Object.keys(TestBackend.mapProperty).length, 0);

        // JSON object with null value
        json =
        "{
          \"k1\": 1,
          \"k2\": null
        }";
        TestBackend.setJsonObject(json);
        mapValueChangedSpy.wait();
        compare(mapValueChangedSpy.count, 4);
        compare(TestBackend.mapProperty["k2"], null)
        compare(TestBackend.mapProperty["k3"], undefined)

        // JSON Array
        json = "[1, 2, 3]"
        TestBackend.setJsonArray(json)
        listValueChangedSpy.wait()
        compare(listValueChangedSpy.count, 1)
        compare(TestBackend.listProperty.length, 3)
        compare(TestBackend.listProperty[2], 3)

        // Empty array
        json = ""
        TestBackend.setJsonArray(json)
        listValueChangedSpy.wait()
        compare(listValueChangedSpy.count, 2)
        compare(TestBackend.listProperty.length, 0)

        // Empty array
        json = "[]"
        TestBackend.setJsonArray(json)
        listValueChangedSpy.wait()
        compare(listValueChangedSpy.count, 3)
        compare(TestBackend.listProperty.length, 0)

        // String array
        json = "[ \"foo\", \"bar\" ]"
        TestBackend.setJsonArray(json)
        listValueChangedSpy.wait()
        compare(listValueChangedSpy.count, 4)
        compare(TestBackend.listProperty.length, 2)
        compare(TestBackend.listProperty[1], "bar")

        // Heterogeneous array with null
        json = "[\"foo\", 1, 3.1, { \"k1\": 1 }, null]"
        TestBackend.setJsonArray(json)
        listValueChangedSpy.wait()
        compare(listValueChangedSpy.count, 5)
        compare(TestBackend.listProperty.length, 5)
        compare(TestBackend.listProperty[4], null)
    }

    function test_toMap() {
        TestBackend.setObjectMapPublicOnly()
        var map = TestBackend.mapProperty
        verify(Object.keys(map).length, 24) // number of public fields
        compare(map.parentInt, 12)
        compare(map.parentString, "€€parent")
        compare(map.childint, 7)
        compare(map.childInteger, 8)
        compare(map.childfloat, 3.14)
        compare(map.childFloat, 4.14)
        compare(map.childdouble, 5.14)
        compare(map.childDouble, 6.14)
        compare(map.childshort, 2)
        compare(map.childShort, 3)
        compare(map.childboolean, true)
        compare(map.childBoolean, false)
        compare(map.childlong, 9)
        compare(map.childLong, 10)
        compare(map.childbyte, 11)
        compare(map.childByte, 12)
        compare(map.childchar, 'a')
        compare(map.childCharacter, 'b')
        compare(map.childString, "€€child")
        compare(map.childURI, "https://example.com")
        compare(map.childList[0], "tag")
        compare(map.childList[1], 1)
        compare(map.childList[2], true)
        compare(map.childList[3].nestedString, "€€nested")
        compare(map.childList[3].nestedInt, 123)
        compare(map.childMap.k1, "v1")
        compare(map.childMap.k2, 2)
        compare(map.childMap.k3.nestedString, "€€nested")
        compare(map.childMap.k3.nestedInt, 123)
        compare(map.childEnum.rgb, "#FF0000")
        compare(map.childEnum.ordinal, 0)
        compare(map.childEnum.name, "RED")
        compare(map.childNestedClass.nestedString, "€€nested")
        compare(map.childNestedClass.nestedInt, 123)
        verify(map.childPrivateString === undefined)
        verify(map.parentProtectedString === undefined)

        // Null should yield an empty map
        TestBackend.setNullObjectMap()
        compare(Object.keys(TestBackend.mapProperty).length, 0)

        // Include private and protected members
        TestBackend.setObjectMapIncludeNonPublic()
        var map = TestBackend.mapProperty
        compare(Object.keys(map).length, 26)
        compare(map.childPrivateString, "shh")
        compare(map.parentProtectedString, "parentProtected")
    }

    function test_toMap_kotlin() {
        KotlinTestBackend.setObjectMapPublicOnly()
        var map = KotlinTestBackend.mapProperty
        compare(Object.keys(map).length, 18)

        compare(map.parentInt, 12)
        compare(map.parentString, "€€parent")

        compare(map.childint, 7)
        compare(map.childfloat, 3.14)
        compare(map.childdouble, 4.14)
        compare(map.childshort, 2)
        compare(map.childboolean, true)
        compare(map.childlong, 9)
        compare(map.childbyte, 11)
        compare(map.childchar, "a")
        compare(map.childString, "€€child")
        compare(map.childURI, "https://example.com")

        compare(map.childList[0], "tag")
        compare(map.childList[1], 1)
        compare(map.childList[2], true)
        compare(map.childList[3].nestedString, "€€nested")
        compare(map.childList[3].nestedInt, 123)
        compare(map.childMap.k1, "v1")
        compare(map.childMap.k2, 2)
        compare(map.childMap.k3.nestedString, "€€nested")
        compare(map.childMap.k3.nestedInt, 123)

        compare(map.childEnum.rgb, "#FF0000")
        compare(map.childEnum.ordinal, 0)
        compare(map.childEnum.name, "RED")

        verify(map.childBoolean !== undefined)
        verify(map.isChildBoolean !== undefined)
        compare(map.childBoolean, map.isChildBoolean)

        // Null should yield an empty map
        KotlinTestBackend.setNullObjectMap()
        compare(Object.keys(KotlinTestBackend.mapProperty).length, 0)

        // Include private and protected members
        KotlinTestBackend.setObjectMapIncludeNonPublic()
        map = KotlinTestBackend.mapProperty
        compare(Object.keys(map).length, 20)
        compare(map.childPrivateString, "shh")
        compare(map.parentProtectedString, "parentProtected")
    }

    function test_nonAsciiPropertyName() {
        compare(TestBackend["nonAs€iiäöProperty"], 123)
        TestBackend["nonAs€iiäöProperty"] = 234
        nonAsciiValueChangedSpy.wait()
        compare(nonAsciiValueChangedSpy.count, 1)
        compare(TestBackend["nonAs€iiäöProperty"], 234)
    }
}
