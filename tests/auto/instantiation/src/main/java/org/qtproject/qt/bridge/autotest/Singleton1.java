/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.core.QtProperty;
import org.qtproject.qt.bridge.core.QtListModel;

@QMLRegistrable(singleton = true)
public class Singleton1 {

    public QtProperty<Integer> var1 = new QtProperty<>(10);
    // For testing garbage collection native cleanup (see main())
    public final QtListModel<String> list1 = new QtListModel<>();

    public void incrementVar1() {
        var1.setValue(var1.getValue() + 1);
    }
}
