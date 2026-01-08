/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge;


import org.qtproject.qt.bridge.core.QtListModel;
import org.qtproject.qt.bridge.core.QtProperty;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.annotations.QMLSignals;

import java.util.Arrays;

// @start region="qmlregistrable-singleton"
@QMLRegistrable(singleton = true)
public class MySingleton {
    // @end
    public interface QmlCallback {
        void somethingHappened();
    }
    @QMLSignals
    QmlCallback qmlCallback;

    // @start region=qtproperty-declaration
    public final QtProperty<String> greeting = new QtProperty<>("Hello");
    {
        greeting.onValueChanged(() -> { System.out.println("Greeting changed to: " + greeting.getValue()); });
    }
    // @end

    // @start region="qtlistmodel-declaration"
    public final QtListModel<Integer> numbers = new QtListModel<>(Arrays.asList(1, 2, 3, 4));

    {
        // Observe changes
        numbers.onSizeChanged(() -> { System.out.println("List size changed to: " + numbers.size()); });
        numbers.onChanged((int start, int count) -> System.out.println("List item value changed at index: " + start));
    }
    // @end

    public void changeGreeting(String newValue) {
        greeting.setValue(newValue);
    }
    public void doSomething() {
        qmlCallback.somethingHappened();
    }
    public void addToNumbers(Integer number) {
        numbers.appendItem(number);
    }

    {
        // @start region="qtlistmodel-onInserted"
        numbers.onInserted((int start, int count) -> { System.out.println("Item inserted at: " + start); });
        // @end
        // @start region="qtlistmodel-onRemoved"
        numbers.onRemoved((int start, int count) -> { System.out.println("Item removed at: " + start); });
        // @end
        // @start region="qtlistmodel-onSizeChanged"
        numbers.onSizeChanged(() -> { System.out.println("Size changed to: " + numbers.size()); });
        // @end
        // @start region="qtlistmodel-onReset"
        numbers.onReset(() -> { System.out.println("List reset."); });
        // @end
        // @start region="qtlistmodel-onChanged"
        numbers.onChanged((int start, int count) -> { System.out.println("Value changed at index: " + start); });
        // @end
    }

    @SuppressWarnings("unused")
    private void _listModelSizeChanged() throws Exception {
        // @start region="qtlistmodel-sizeChanged-with-sub"
        AutoCloseable sub = numbers.onSizeChanged(() -> {
            System.out.println("List size changed to: " + numbers.size());
        });
        // ... later
        sub.close();
        // @end
    }
    @SuppressWarnings("unused")
    private void _propertyChangeNotification() throws Exception {
        // @start region="qtproperty-valueChanged-with-sub"
        AutoCloseable sub = greeting.onValueChanged(() -> {
            System.out.println("Property changed to: " + greeting.getValue());
        });
        // ... later
        sub.close();
        // @end
    }
}
