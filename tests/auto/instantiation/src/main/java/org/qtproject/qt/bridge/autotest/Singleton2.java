/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.core.QtProperty;

// Registration 'name' omitted on purpose to verify that class name is used:
@QMLRegistrable(singleton = true, module = "AnotherModule")
public class Singleton2 {

    public QtProperty<Integer> var1 = new QtProperty<>(20);

    public void incrementVar1() {
        var1.setValue(var1.getValue() + 1);
    }
}
