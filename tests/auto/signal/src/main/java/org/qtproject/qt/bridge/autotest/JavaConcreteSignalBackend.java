/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.annotations.QMLSignal;

// Concrete @QMLSignal methods with all three Java visibility levels.
@QMLRegistrable(singleton = true)
public class JavaConcreteSignalBackend {

    @QMLSignal
    public void publicSignal(int value) {}

    @QMLSignal
    protected void protectedSignal(int value) {}

    @QMLSignal
    void packagePrivateSignal(int value) {}

    public void triggerPublicSignal(int v) {
        publicSignal(v);
    }

    public void triggerProtectedSignal(int v) {
        protectedSignal(v);
    }

    public void triggerPackagePrivateSignal(int v) {
        packagePrivateSignal(v);
    }
}
