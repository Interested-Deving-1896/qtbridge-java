/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge.mymodule1;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.annotations.QMLSignals;
import org.qtproject.qt.bridge.core.QtProperty;

@QMLRegistrable(singleton=false, module="Two.Wheels")
public class Bike {
    public interface QmlCallback {
        void pedalled();
    }
    @QMLSignals
    QmlCallback qmlCallback;

    public final QtProperty<String> stringProp = new QtProperty<>("stringProp");
    public final QtProperty<String> intProp = new QtProperty<>("intProp");

    public void pedal() {
        qmlCallback.pedalled();
    }
}
