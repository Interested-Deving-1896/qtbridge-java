/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

import org.qtproject.qt.bridge.utils.loader.NativeLibraryLoader;

import java.lang.ref.Cleaner;
import java.lang.reflect.Method;

class QtObject {
    static {
        NativeLibraryLoader.loadLibrary();
    }
    private final long nativeHandle;
    // A cleaner that runs when userObject becomes phantom-reachable (GC’d)
    private static final Cleaner userObjectCleaner = Cleaner.create();

    // Connect property value-change notification to QML-side signal emission
    void connectPropertyNotifyToSignalEmission(String name, QtProperty<?> p) {
        p.onValueChanged(() -> nativeEmitSignal(nativeHandle, name + "Changed()"));
    }

    private final void initializeMeta(Object userObject) {
        Class<?> userClass = userObject.getClass();
        ClassLoader cl = userClass.getClassLoader();
        String metaClassName = "org.qtproject.qt.bridge.core." + userClass.getSimpleName() + "_QtMeta";
        // Find generated UserClass_QtMeta class and invoke it to register meta
        final Class<?> metaClass;
        try {
            metaClass = Class.forName(metaClassName, true, cl);
            Method registrationMethod = metaClass.getMethod("registerMeta", QtObject.class, Object.class);
            registrationMethod.invoke(null, this, userObject);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Missing generated private QtObject for " + userClass.getName() +
                            " (" + metaClassName + "). Did KSP run?", e);
        }
    }

    public QtObject(Object userObject, long handle, boolean ownedByQml) {
        if (userObject == null || handle == 0)
            throw new NullPointerException("User object for QML registration must not be null");
        nativeHandle = handle;
        initializeMeta(userObject);
        if (!ownedByQml) { // Dispose resources from Java-side only if Java-side ownership
            userObjectCleaner.register(userObject, () -> { nativeDispose(nativeHandle); });
        }
    }

    void emitSignal(String signature, Object... args) {
        nativeEmitSignal(nativeHandle, signature, args);
    }

    // These add* methods are called by the generated metaclass during
    // metaobject initialization (generated _QtMeta). These methods add methods, signal,
    // and properties to the underlying QtObjectWrapper's dynamic metaobject.
    void addInvokable(String javaSignature, String javaReturnType,
                      String cppSignature, String cppReturnType, boolean retIsPrimitive,
                      boolean[] parmIsPrimitive) {
        nativeAddInvokable(nativeHandle, javaSignature, javaReturnType,
                           cppSignature, cppReturnType, retIsPrimitive, parmIsPrimitive);
    }
    void addSignal(String javaSignature, String cppSignature, String[] cppParamTypes) {
        nativeAddSignal(nativeHandle, javaSignature, cppSignature, cppParamTypes);
    }
    void addProperty(QProperty property) {
        nativeAddProperty(nativeHandle, property.name(), property.javaType(),
                property.cppType(), property.writeable(), property.readable(),
                property.notificationSignal(), property.constant());
    }

    // Native functions
    private static native void nativeDispose(long handle);
    private native void nativeAddInvokable(long handle, String javaSignature, String javaReturnType,
                                           String cppSignature, String cppReturnType, boolean retIsPrimitive,
                                           boolean[] parmIsPrimitive);
    private native void nativeAddSignal(long handle, String javaSignature, String cppSignature, String[] cppParamTypes);
    private native void nativeAddProperty(long handle, String name, String javaType,
                                          String cppType, boolean writeable, boolean readable,
                                          String signalSignature, boolean isConstant);
    private native void nativeEmitSignal(long handle, String signalName, Object... args);
}
