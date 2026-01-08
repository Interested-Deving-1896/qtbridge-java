/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.core.QtQmlChildren;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.core.QtProperty;

@QMLRegistrable
public class QmlType1 {
    public QtProperty<Integer> var1 = new QtProperty<>(11);
    public QtProperty<QmlType2> qmlType2Prop1 = new QtProperty<>(null);
    public QtProperty<QmlType2> qmlType2Prop2 = new QtProperty<>(null);
    public QtProperty<QmlType2> qmlType2Prop3 = new QtProperty<>(null);

    public void decrementVar1() {
        var1.setValue(var1.getValue()-1);
    }
    public void incrementVar1() {
        var1.setValue(var1.getValue()+1);
    }
    public void nullQmlType2Prop2() {
        qmlType2Prop2.setValue(null);
    }
    public void assignQmlType2Prop2() {
        qmlType2Prop2.setValue(new QmlType2());
    }

    public int allChildrenCount() {
        return QtQmlChildren.children(this).toArray().length;
    }
    public int qmlType2ChildrenCount() {
        return QtQmlChildren.children(this, QmlType2.class).toArray().length;
    }
    public int parentlessChildrenCount() {
        Object someRandomObject = new Object();
        return QtQmlChildren.children(someRandomObject).toArray().length;
    }
}
