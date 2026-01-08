/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.annotations.QMLSignals;
import org.qtproject.qt.bridge.core.QtListModel;
import org.qtproject.qt.bridge.core.QtProperty;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@QMLRegistrable(singleton = true)
public class BenchmarkBackend {

    public interface QMLCallback {
        void withoutParameters();
        void withParameters(int a, int b);
    }
    @QMLSignals
    QMLCallback qmlCallback;

    public final QtListModel<String> numbers = new QtListModel<>();
    List<String> items = IntStream.range(0, 20)
            .mapToObj(i -> "Item " + i)
            .collect(Collectors.toList());

    public final QtProperty<String> stringProperty = new QtProperty<>("Hello");
    public final QtProperty<Integer> intProperty = new QtProperty<>(123);

    public void initialize() {
        numbers.setItems(items);
        stringProperty.setValue("Hello");
        intProperty.setValue(123);
    }

    public void setItems() { numbers.setItems(items); }
    public void appendItem(int item) {
        numbers.appendItem("Item " + item);
    }
    public boolean contains(String item) { return numbers.contains(item); }

    public void reset() {
        numbers.reset();
    }
    public int size() {
        return numbers.size();
    }

    public void voidFunction() {}
    public void parameterFunction(int a, int b, String c, String d, int e, int f, String g, int h) {}
    public String returningFunction() {
        return "theReturnValue";
    }
    public void emitingFunction() {
        // Loop for a few times so that signal emission trumps
        // execution time over the calling cost of this function
        for (int i = 0; i < 20; ++i) {
            qmlCallback.withoutParameters();
            qmlCallback.withParameters(1, 2);
        }
    }
}
