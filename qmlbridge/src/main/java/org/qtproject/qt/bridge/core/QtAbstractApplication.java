/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

import org.qtproject.qt.bridge.utils.loader.NativeLibraryLoader;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for all Java -> Qt/QML application entry points.
 *
 * <p>This class is not intended to be instantiated directly. It provides the shared bootstrap
 * that all concrete application types rely on (e.g. {@link QtQuickApplication}).</p>
 */
public abstract class QtAbstractApplication
{
    /**
    * For subclassing only.
    */
    protected QtAbstractApplication() {}

    private static final AtomicBoolean QML_TYPES_REGISTERED = new AtomicBoolean(false);
    static {
        NativeLibraryLoader.loadLibrary();
        if (QML_TYPES_REGISTERED.compareAndSet(false, true)) {
            try {
                Class<?> c = Class.forName("org.qtproject.qt.bridge.core.QtQmlRegistration_QtMeta");
                c.getMethod("registerRegistrablesAsQmlTypes").invoke(null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to register QML types, did KSP run?", e);
            }
        }
    }
}
