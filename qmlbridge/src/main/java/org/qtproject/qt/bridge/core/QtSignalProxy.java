/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

import java.lang.reflect.*;
import java.util.Arrays;

// This class provides a static bind() method, which returns a standard
// dynamic Java proxy instance. The returned instance can be used to replace
// the @QMLSignals interface field; the proxy implements that interface.
// This then means that when user calls the @QMLSignals methods, they are
// actually caught by the proxy, and converted into a qtObject.emitSignal()
// call.
final class QtSignalProxy {
    private QtSignalProxy() {}

    private static String toSignalTypeName(Class<?> clazz) {
        if (clazz.isArray())
            return toSignalTypeName(clazz.getComponentType()) + "[]";
        return clazz.getName();
    }

    @SuppressWarnings("unchecked")
    static <T> T bind(Class<T> signalsInterface, QtObject qtObject) {
        InvocationHandler handler = (proxy, method, args) -> {
            // Convert proxy methodcall data to proper signal signature "name(type1,type2,...)".
            // The signature is used as a cache key to find the right signal
            String signature = method.getName() + "(" +
                    Arrays.stream(method.getParameterTypes())
                            .map(QtSignalProxy::toSignalTypeName)
                            .reduce((a, b) -> a + "," + b).orElse("") +
                    ")";
            qtObject.emitSignal(signature, args == null ? new Object[0] : args);
            return null; // Signal return values are void
        };
        return (T) Proxy.newProxyInstance(
                signalsInterface.getClassLoader(), new Class<?>[]{signalsInterface}, handler);
    }
}
