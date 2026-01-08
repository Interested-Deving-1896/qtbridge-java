/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

// Java-side TestBackend calls these to inform QML-side
public interface QMLCallback {
    void integerPropertyValueChanged();
    void stringListPropertyValueChanged();
    void mapPropertyValueChanged();
    void listPropertyValueChanged();
}
