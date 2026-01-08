/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

import QtQuick
import QtTest

import QtBridge

TestCase {
    id: testCase
    name: "QML Types Test Suite"
    when: windowShown

    QmlType1 {
        id: qmlType1Instance
    }

    QmlType2 {
        id: qmlType2Instance1
    }

    QmlType3 {
        id: qmlType3Instance1
        onQmlCompleted: function (sum) {
            sumFromCompletionHandler = sum
        }
        property int sumFromCompletionHandler : 0
        var1: 1; var2: 1; var3: 1; var4: 1; var5: 1; var6: 1
    }
    // Binding
    property var qmlType2Instance1Var1 : qmlType2Instance1.var1

    QmlType2 {
        id: qmlType2Instance2
    }
    // Binding
    property var qmlType2Instance2Var1 : qmlType2Instance2.var1

    // For dynamic instantiations
    Component {
        id: qmlType1Component
        QmlType1 {}
    }

    Component {
        id: qmlType2Component
        QmlType2 {}
    }

    SignalSpy {
        id: onCallFunctionQmType2Spy
        target: qmlType2Instance1
        signalName: "onCall"
    }

    // Nested types
    QmlType1 {
        id: containing
        qmlType2Prop1 : QmlType2 { id: nested1 }
        qmlType2Prop2 : QmlType2 { id: nested2 }
        qmlType2Prop3 : null
    }

    QmlType2 {
        id: assignee
        var1: 77
    }

    SignalSpy {
        id: qmlType2Prop2ChangedSpy
        target: containing
        signalName: "qmlType2Prop2Changed"
    }

    // Default property
    QmlType1 {
        id: defaultContaining
        QmlType2 { id: defaultNested1 }
        QmlType2 { id: defaultNested2 }
    }

    QmlType1 {
        id: emptyContaining
    }

    SignalSpy {
        id: emptyQmlType2Prop2ChangedSpy
        target: emptyContaining
        signalName: "qmlType2Prop2Changed"
    }

    // Default property children test
    QmlType1 {
        id: childrenContainer
        QmlType2{ id: child1 }
        QmlType2{ id: child2 }
        QmlType2{ id: child3 }
        Rectangle{ id: child4 }
        QmlType1{ id: child5 }
    }

    // Garbage collection at init() and cleanup() are done
    // to make sure it doesn't collect items that it shouldn't (a test crash)
    function init() {
        TestController.garbageCollect();
    }

    function cleanup() {
        TestController.garbageCollect();
    }

    function test_qmlComplete() {
        // By now the component is already complete. Test that
        // the properties were initialized when the element sent
        // the component completion signal
        compare(qmlType3Instance1.sumFromCompletionHandler, 6);
    }

    function test_children() {
        // All children:
        compare(childrenContainer.children.length, 5);
        // Rectangle is filtered out as non-QMLRegistrable:
        compare(childrenContainer.allChildrenCount(), 4);
        // Filter out by QmlType2
        compare(childrenContainer.qmlType2ChildrenCount(), 3);
        // Childless container
        compare(emptyContaining.allChildrenCount(), 0);
        // Call with non-QMLRegistrable parent
        ignoreWarning("No proxy found for parent, cannot look up children")
        compare(childrenContainer.parentlessChildrenCount(), 0);

    }

    function test_assign_from_java() {
        emptyQmlType2Prop2ChangedSpy.clear()
        verify(emptyContaining.qmlType2Prop1 === null)
        verify(emptyContaining.qmlType2Prop2 === null)
        verify(emptyContaining.qmlType2Prop3 === null)
        // Assign a value at Java-side
        emptyContaining.assignQmlType2Prop2()
        wait(1)
        compare(emptyQmlType2Prop2ChangedSpy.count, 1)
        verify(emptyContaining.qmlType2Prop2 !== null)
        compare(emptyContaining.qmlType2Prop2.var1, 0)
        emptyContaining.qmlType2Prop2.var1 = 11
        compare(emptyContaining.qmlType2Prop2.var1, 11)
        // Null the value at Java-side
        emptyContaining.nullQmlType2Prop2()
        wait(1)
        compare(emptyQmlType2Prop2ChangedSpy.count, 2)
        verify(emptyContaining.qmlType2Prop2 === null)
    }

    function test_default_property() {
        compare(defaultContaining.children.length, 2)
        compare(defaultNested1.var1, 0)
        compare(defaultNested2.var1, 0)
        defaultNested1.var1 = 123
        defaultNested2.var1 = 456
        compare(defaultNested1.var1, 123)
        compare(defaultNested2.var1, 456)
    }

    function test_nested_types() {
        compare(containing.qmlType2Prop1, nested1)
        compare(containing.qmlType2Prop2, nested2)
        compare(containing.qmlType2Prop3, null)
        qmlType2Prop2ChangedSpy.clear()
        // Test initial values
        compare(containing.var1, 11)
        compare(nested1.var1, 0)
        compare(nested2.var1, 0)
        // Test setting values
        containing.var1 = 123
        nested1.var1 = 456
        nested2.var1 = 789
        compare(containing.var1, 123)
        compare(nested1.var1, 456)
        compare(nested2.var1, 789)

        // Test assigning element
        containing.qmlType2Prop2 = assignee
        wait(1)
        compare(qmlType2Prop2ChangedSpy.count, 1)
        compare(containing.qmlType2Prop2, assignee)
        compare(containing.qmlType2Prop2.var1, 77)
        containing.qmlType2Prop2.var1 = 88
        compare(assignee.var1, 88)
        compare(containing.qmlType2Prop2.var1, 88)
        // Assign to null and back
        containing.qmlType2Prop2 = null
        compare(containing.qmlType2Prop2, null)
        wait(1)
        compare(qmlType2Prop2ChangedSpy.count, 2)
        containing.qmlType2Prop2 = assignee
        wait(1)
        compare(qmlType2Prop2ChangedSpy.count, 3)
        compare(containing.qmlType2Prop2, assignee)
        // Set null on the Java-side
        containing.nullQmlType2Prop2()
        compare(containing.qmlType2Prop2, null)
        wait(1)
        compare(qmlType2Prop2ChangedSpy.count, 4)
    }

    function test_a_qmlType1_initial_value() {
        compare(qmlType1Instance.var1, 11)
    }

    function test_b_qmlType1_increment() {
        var initialValue = qmlType1Instance.var1
        qmlType1Instance.incrementVar1()
        compare(qmlType1Instance.var1, initialValue + 1)
    }

    function test_qmlType1_property_assignment() {
        qmlType1Instance.var1 = 50
        compare(qmlType1Instance.var1, 50)
    }

    function test_a_qmlType2_initial_values() {
        compare(qmlType2Instance1.var1, 0)
        compare(qmlType2Instance2.var1, 0)
        compare(qmlType2Instance1.var1, testCase.qmlType2Instance1Var1)
        compare(qmlType2Instance2.var1, testCase.qmlType2Instance2Var1)
    }

    function test_qmlType2_increment_method() {
        var initialValue1 = qmlType2Instance1.var1
        var initialValue2 = qmlType2Instance2.var1
        // Change instance1
        qmlType2Instance1.increment()
        wait(1) // visit eventloop so that qmlType2Instance1Var1 bindings evaluate
        compare(qmlType2Instance1.var1, initialValue1 + 1)
        compare(qmlType2Instance1.var1, testCase.qmlType2Instance1Var1)
        // No changes to instance2
        compare(qmlType2Instance2.var1, initialValue2)
        compare(qmlType2Instance2.var1, testCase.qmlType2Instance2Var1)

        // Change instance2
        qmlType2Instance2.increment()
        wait(1)
        compare(qmlType2Instance2.var1, initialValue2 + 1)
        compare(qmlType2Instance2.var1, testCase.qmlType2Instance2Var1)
        // No changes to instance1
        compare(qmlType2Instance1.var1, initialValue1 + 1)
        compare(qmlType2Instance1.var1, testCase.qmlType2Instance1Var1)
    }

    function test_qmlType2_property_assignment() {
        qmlType2Instance1.var1 = 10
        compare(qmlType2Instance1.var1, 10)
        wait(1)
        compare(qmlType2Instance1Var1, 10)
        qmlType2Instance2.var1 = 20
        wait(1)
        compare(qmlType2Instance2.var1, 20)
        compare(qmlType2Instance2Var1, 20)
    }

    function test_qmlType2_updateVar1_method() {
        qmlType2Instance1.updateVar1(25)
        wait(1)
        compare(qmlType2Instance1.var1, 25)
        compare(qmlType2Instance1Var1, 25)

        qmlType2Instance2.updateVar1(35)
        wait(1)
        compare(qmlType2Instance2.var1, 35)
        compare(qmlType2Instance2Var1, 35)
        compare(qmlType2Instance1Var1, 25) // no change
    }

    function test_qmlType2_combined_operations() {
        qmlType2Instance1.updateVar1(5)
        compare(qmlType2Instance1.var1, 5)
        qmlType2Instance1.increment()
        compare(qmlType2Instance1.var1, 6)
    }

    function test_qmlType2_trigger_signal() {
        qmlType2Instance1.triggerCall()
        onCallFunctionQmType2Spy.wait()
        compare(onCallFunctionQmType2Spy.count , 1)
    }

    function test_dynamic_instances() {
        var instance1 = qmlType2Component.createObject()
        verify(instance1)
        compare(instance1.var1, 0)
        instance1.increment()
        compare(instance1.var1, 1)

        var instance2 = qmlType2Component.createObject(null, { var1: 321 }) // null is parent
        verify(instance2)
        compare(instance2.var1, 321)
        instance2.increment()
        compare(instance2.var1, 322)

        instance1.destroy()
        instance2.destroy()
    }

    function test_garbage_collection() {
        TestController.garbageCollect();
        for (let i = 0; i < 50; ++i) {
            var instance1 = qmlType2Component.createObject();
            TestController.garbageCollect(); // Java GC
            gc() // JavaScript GC
            instance1.increment();
            compare(instance1.var1, 1);
            var instance2 = qmlType1Component.createObject();
            TestController.garbageCollect();
            gc()
            instance2.incrementVar1();
            compare(instance2.var1, 12);
        }
    }
}
