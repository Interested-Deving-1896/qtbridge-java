/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge;

// @start region="qmlregistrable-fullclass-creatable"

import org.qtproject.qt.bridge.core.QtQmlChildren;
import org.qtproject.qt.bridge.core.QtListModel;
import org.qtproject.qt.bridge.core.QtProperty;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.annotations.QMLSignals;
import org.qtproject.qt.bridge.annotations.QMLComplete;

import java.util.Arrays;
import java.util.List;

// Default QMLRegistrable values register a creatable type 'MyType' 'QtBridge' module
@QMLRegistrable
public class MyType {
    // @start region="qmlsignals-usage"
    // Callbacks (signals) that are used to invoke QML from Java
    public interface QmlCallback {
        void somethingHappened();
    }
    @QMLSignals
    QmlCallback qmlCallback;
    // @end

    // String exposed to QML, can be edited both Java- and QML side
    public final QtProperty<String> greeting = new QtProperty<>("Hello");
    // List model exposed to QML, can be edited both Java- and QML side
    public final QtListModel<Integer> numbers = new QtListModel<>(Arrays.asList(1, 2, 3, 4));

    {
        // Optionally observe changes on Java-side
        greeting.onValueChanged(() -> { System.out.println("Greeting changed to: " + greeting.getValue()); });
        numbers.onSizeChanged(() -> System.out.println("List size changed to: " + numbers.size()));
        numbers.onChanged((int start, int count) -> System.out.println("List item value changed at index: " + start));
    }

    // Methods that are callable from QML
    public void changeGreeting(String newValue) {
        greeting.setValue(newValue);
    }
    public void doSomething() {
        qmlCallback.somethingHappened();
    }
    public void addToNumbers(Integer number) {
        numbers.appendItem(number);
    }

    // @start region="qml-complete"
    @QMLComplete
    public void onComplete() {
        // All QtProperty values and QML children are now set by QML
        System.out.println("QML Complete");
        // @start region="qml-children"
        List<MyChildType> children = QtQmlChildren.children(this, MyChildType.class);
        // @end
    }
    // @end
}
// @end
