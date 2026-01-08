/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.annotations.QMLSignals;
import org.qtproject.qt.bridge.core.QtListModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@QMLRegistrable(name = "TestBackend", singleton = true)
public class TestBackend {
    public TestBackend() {}

    @QMLSignals
    QMLCallback qmlCallback;

    private final List<String> initialValues =
            new ArrayList<>(Arrays.asList("Zero", "One", "Two", "Three", "Four"));
    public final QtListModel<String> numbers = new QtListModel<>();

    // Test cases call this function to set the initial values back
    public void resetModelToInitialTestState() {
        numbers.setItems(initialValues);
    }
    public void resetModel() {
        numbers.reset();
    }
    public void updateItemAt(int index, String item) {
        numbers.updateItemAt(index, item);
    }
    public boolean contains(String item) {
        return numbers.contains(item);
    }
    public void appendItem(String number) {
        numbers.appendItem(number);
    }
    public void removeItemAt(int index) {
        numbers.removeItemAt(index);
    }
    public void removeItem(String number) {
        numbers.removeItem(number);
    }
    public int size() {
        return numbers.size();
    }

    private ArrayList<AutoCloseable> observers = new ArrayList<>();
    public void startObserving() {
        if (!observers.isEmpty())
            return;
        observers.add(numbers.onInserted((int s, int c) -> { qmlCallback.modelInserted(s, c); }));
        observers.add(numbers.onRemoved((int s, int c) -> { qmlCallback.modelRemoved(s, c); }));
        observers.add(numbers.onChanged((int s, int c) -> { qmlCallback.modelChanged(s, c); }));
        observers.add(numbers.onReset(() -> { qmlCallback.modelReset(); }));
        observers.add(numbers.onSizeChanged(() -> { qmlCallback.modelSizeChanged(); }));
    }

    public void stopObserving() {
        for (AutoCloseable observer : observers) {
            try { observer.close(); } catch(Exception ignored) {}
        }
        observers.clear();
    }
}
