/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge.mymodule1;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.annotations.QMLSignals;
import org.qtproject.qt.bridge.core.QtProperty;

@QMLRegistrable(singleton=false, module="Animals")
public class Dog {
    public interface QmlCallback {
        void barked();
    }
    @QMLSignals
    QmlCallback qmlCallback;

    public final QtProperty<String> stringProp = new QtProperty<>("stringProp");
    public final QtProperty<String> intProp = new QtProperty<>("intProp");

    public void bark() {
        qmlCallback.barked();
    }
}
