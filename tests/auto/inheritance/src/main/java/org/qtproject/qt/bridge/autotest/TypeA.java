/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.core.QtProperty;

class Base {
    public QtProperty<Integer> propBase = new QtProperty<Integer>(123);
    public int invokableBase() { return 123; }
}

@QMLRegistrable()
public class TypeA extends Base {
    public QtProperty<Integer> propA = new QtProperty<>(456);
    public int invokableA() { return 456; }
}
