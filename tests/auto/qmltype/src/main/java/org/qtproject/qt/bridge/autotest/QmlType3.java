/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLComplete;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.annotations.QMLSignals;
import org.qtproject.qt.bridge.core.QtProperty;

@QMLRegistrable
public class QmlType3 {
    public interface QMLCallback {
        void qmlCompleted(int sumOfProperties);
    }
    @QMLSignals
    QMLCallback qmlCallback;

    public QtProperty<Integer> var1 = new QtProperty<>(0);
    public QtProperty<Integer> var2 = new QtProperty<>(0);
    public QtProperty<Integer> var3 = new QtProperty<>(0);
    public QtProperty<Integer> var4 = new QtProperty<>(0);
    public QtProperty<Integer> var5 = new QtProperty<>(0);
    public QtProperty<Integer> var6 = new QtProperty<>(0);

    @QMLComplete
    public void onCompleted() {
        qmlCallback.qmlCompleted(var1.getValue() + var2.getValue()
                + var3.getValue() + var4.getValue() + var5.getValue() + var6.getValue());
    }
}
