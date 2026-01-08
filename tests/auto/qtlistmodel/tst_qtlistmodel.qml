/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

import QtQuick
import QtTest

import QtBridge

TestCase {
    name: "QtListModel"
    when: windowShown

    ListView {
        id: view
        width: 100
        height: 100
        model: TestBackend.numbers
        delegate: Item {
            id: delegate
            width: 5
            height: 5
            required property int index
            required property var model
            function setValue(newValue) {
                delegate.model.value = newValue
            }
        }
    }

    SignalSpy {
        id: insertedSpy
        target: TestBackend
        signalName: "onModelInserted"
    }
    SignalSpy {
        id: removedSpy
        target: TestBackend
        signalName: "onModelRemoved"
    }
    SignalSpy {
        id: changedSpy
        target: TestBackend
        signalName: "onModelChanged"
    }
    SignalSpy {
        id: resetSpy
        target: TestBackend
        signalName: "onModelReset"
    }
    SignalSpy {
            id: sizeChangedSpy
            target: TestBackend
            signalName: "onModelSizeChanged"
    }

    function clearSpies() {
        insertedSpy.clear();
        removedSpy.clear();
        changedSpy.clear();
        resetSpy.clear();
        sizeChangedSpy.clear();
    }
    function init() {
        TestBackend.resetModelToInitialTestState();
        TestBackend.startObserving();
        clearSpies();
    }

    function cleanup() {
        TestBackend.stopObserving();
    }

    function test_initialValues() {
        tryCompare(view, "count", 5);
        verify(view.itemAtIndex(0));
        compare(view.itemAtIndex(0).model.value, "Zero");
        verify(view.itemAtIndex(1));
        compare(view.itemAtIndex(1).model.value, "One");
        verify(view.itemAtIndex(2));
        compare(view.itemAtIndex(2).model.value, "Two");
        verify(view.itemAtIndex(3));
        compare(view.itemAtIndex(3).model.value, "Three");
        verify(view.itemAtIndex(4));
        compare(view.itemAtIndex(4).model.value, "Four");
    }


    function test_changeValueFromQml() {
        // Change value at index 3
        view.itemAtIndex(4).setValue("IV");
        compare(view.itemAtIndex(4).model.value, "IV");
        changedSpy.wait()
        compare(changedSpy.count, 1);
        compare(changedSpy.signalArguments[0].length, 2); // two arguments
        compare(changedSpy.signalArguments[0][0], 4);     // start position
        compare(changedSpy.signalArguments[0][1], 1);     // count
        compare(sizeChangedSpy.count, 0); // size shouldn't change
    }

    function test_changeValueFromJava() {
        TestBackend.updateItemAt(3, "III");
        compare(view.itemAtIndex(3).model.value, "III");
        changedSpy.wait()
        compare(changedSpy.count, 1);
        compare(changedSpy.signalArguments[0].length, 2); // two arguments
        compare(changedSpy.signalArguments[0][0], 3);     // start position
        compare(changedSpy.signalArguments[0][1], 1);     // count

        // Invalid updates should be no-ops
        clearSpies();
        TestBackend.updateItemAt(-1, "MinusOne");
        TestBackend.updateItemAt(99, "Luftballoons");
        wait(10);
        compare(changedSpy.count, 0);
    }

    function test_appendItems() {
        // Add an item on Java-side
        TestBackend.appendItem("Five");
        compare(view.count, 6);
        // wait for the delegate to be instantiated
        tryVerify(function() { return view.itemAtIndex(5); });
        compare(view.itemAtIndex(5).model.value, "Five");
        insertedSpy.wait()
        compare(insertedSpy.count, 1);
        compare(insertedSpy.signalArguments[0].length, 2); // two arguments
        compare(insertedSpy.signalArguments[0][0], 5);     // start position
        compare(insertedSpy.signalArguments[0][1], 1);     // count
        sizeChangedSpy.wait()
        compare(sizeChangedSpy.count, 1);

        // Add empty item
        TestBackend.appendItem("");
        compare(view.count, 7);
        // wait for the delegate to be instantiated
        tryVerify(function() { return view.itemAtIndex(6); });
        compare(view.itemAtIndex(6).model.value, "");
    }

    function test_removeItems() {
        // Remove an item by value at Java-side
        TestBackend.removeItem("Four");
        compare(view.count, 4);
        compare(TestBackend.size(), 4);
        verify(!TestBackend.contains("Four"));
        // wait for delegate to be torn down
        tryVerify(function() { return !view.itemAtIndex(4); });
        removedSpy.wait();
        compare(removedSpy.count, 1);
        compare(removedSpy.signalArguments[0].length, 2); // two arguments
        compare(removedSpy.signalArguments[0][0], 4);     // start position
        compare(removedSpy.signalArguments[0][1], 1);     // count
        sizeChangedSpy.wait()
        compare(sizeChangedSpy.count, 1);

        // Remove an item by index at Java-side
        TestBackend.removeItemAt(2);
        compare(view.count, 3);
        compare(TestBackend.size(), 3);
        verify(!TestBackend.contains("Two"));
        // wait for delegate to be torn down
        tryVerify(function() { return !view.itemAtIndex(3); });
        removedSpy.wait();
        compare(removedSpy.count, 2);
        compare(removedSpy.signalArguments[1].length, 2); // two arguments
        compare(removedSpy.signalArguments[1][0], 2);     // start position
        compare(removedSpy.signalArguments[1][1], 1);     // count
        sizeChangedSpy.wait()
        compare(sizeChangedSpy.count, 2);

        // Invalid removals should be no-ops
        clearSpies();
        TestBackend.removeItemAt(999);
        TestBackend.removeItemAt(-1);
        TestBackend.removeItem("i_dont_exist");
        TestBackend.removeItem("");
        wait(10);
        compare(view.count, 3);
        compare(TestBackend.size(), 3);
        compare(removedSpy.count, 0);
        compare(sizeChangedSpy.count, 0);
    }

    function test_reset() {
        // Reset model
        TestBackend.resetModel();
        resetSpy.wait();
        compare(resetSpy.count, 1);
        sizeChangedSpy.wait();
        compare(sizeChangedSpy.count, 1);
        compare(view.count, 0);
    }

    function test_stopObserving() {
        // Reset model and verify that no callbacks are received
        // when we stop observing
        TestBackend.stopObserving();
        TestBackend.resetModel();
        wait(10);
        compare(resetSpy.count, 0);
        compare(sizeChangedSpy.count, 0);
    }

    function test_contains() {
        verify(TestBackend.contains("Zero"));
        verify(TestBackend.contains("One"));
        verify(TestBackend.contains("Two"));
        verify(TestBackend.contains("Three"));
        verify(TestBackend.contains("Four"));
        verify(!TestBackend.contains("shouldnt_exist"));
    }

}
