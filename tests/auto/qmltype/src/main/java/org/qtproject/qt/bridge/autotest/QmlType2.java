/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.annotations.QMLSignals;
import org.qtproject.qt.bridge.core.QtListModel;
import org.qtproject.qt.bridge.core.QtProperty;

import java.util.ArrayList;

@QMLRegistrable
public class QmlType2 {
    @QMLSignals
    TypeCallback callback;
    public QtProperty<Integer> var1 = new QtProperty<>(0);
    private final QtListModel<String> list1 = new QtListModel<>();

    public void increment() {
        var1.setValue(var1.getValue() + 1);
    }

    public void updateVar1(Integer newVal) {
        var1.setValue(newVal);
    }
    public void triggerCall() {
        callback.call();
    }

    private ArrayList<AutoCloseable> observers = new ArrayList<>();

    public void addToMyList(String input) {
        list1.appendItem(input);
        observers.add(list1.onChanged((int a, int c) -> callback.listUpdated(c)));
    }

    public void startObserving() {
        if (!observers.isEmpty())
            return;
        observers.add(list1.onChanged((int a, int c) -> callback.listUpdated(c)));
    }

    public void stopObserving() {
        for (AutoCloseable observer : observers) {
            try {observer.close();} catch (Exception ignored) {}
        }
        observers.clear();
    }
}
