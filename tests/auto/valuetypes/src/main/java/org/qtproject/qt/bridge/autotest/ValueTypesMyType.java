/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.core.QtProperty;

@QMLRegistrable(name = "MyType")
public class ValueTypesMyType {
    public ValueTypesMyType() {}
    public ValueTypesMyType(Integer value) { intProp.setValue(value); }

    public QtProperty<Integer> intProp = new QtProperty<>(0);
}
