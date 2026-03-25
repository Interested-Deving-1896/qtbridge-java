/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.annotations.QMLSignal;

// Abstract @QMLSignal methods with all three Java visibility levels.
// Due to these abstract methods, the class itself must also be abstract
@QMLRegistrable(singleton = true)
public abstract class JavaAbstractSignalBackend {

    @QMLSignal
    public abstract void publicSignal(int value);

    @QMLSignal
    protected abstract void protectedSignal(int value);

    @QMLSignal
    abstract void packagePrivateSignal(int value);

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
