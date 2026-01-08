/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;
import org.qtproject.qt.bridge.utils.loader.NativeLibraryLoader;

// Helpers for preparing QML registrations
final class QtQmlRegistration {

    static {
        NativeLibraryLoader.loadLibrary();
    }

    static void registerQmlType(
            String typeName,
            String moduleName,
            boolean isSingleton,
            String qmlCompleteMethod,
            Class<?> userClass
    ) {
        nativeRegisterQmlType(typeName, moduleName, isSingleton, qmlCompleteMethod, userClass);
    }

    private static native void nativeRegisterQmlType(
            String name,
            String module,
            boolean isSingleton,
            String qmlCompleteMethod,
            Class<?> userClass
    );
}
