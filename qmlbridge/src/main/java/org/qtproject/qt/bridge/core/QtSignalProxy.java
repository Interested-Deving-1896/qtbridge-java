/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

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

    private static String buildSignalSignature(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        StringBuilder signature = new StringBuilder(method.getName()).append("(");
        for (int i = 0; i < parameterTypes.length; ++i) {
            if (i > 0)
                signature.append(",");
            signature.append(toSignalTypeName(parameterTypes[i]));
        }
        signature.append(")");
        return signature.toString();
    }

    @SuppressWarnings("unchecked")
    static <T> T bind(Class<T> signalsInterface, QtObject qtObject) {
        // Create signatures of each signal. Must match the signature we
        // generate in ClassModelGenerator as the signature is used as the cache key
        final Map<Method, String> mutableSignatureCache = new HashMap<>();
        for (Method method : signalsInterface.getMethods()) {
            if (method.getDeclaringClass() != Object.class)
                mutableSignatureCache.put(method, buildSignalSignature(method));
        }
        // Copy signals in an immutable map (captured into handler lambda)
        final Map<Method, String> signatureCache = Map.copyOf(mutableSignatureCache);

        InvocationHandler handler = (proxy, method, args) -> {
            // Handle general Object methods so what we won't try to use
            // them as signals (would throw).
            if (method.getDeclaringClass() == Object.class) {
                String name = method.getName();
                if ("toString".equals(name))
                    return "QtSignalProxy(" + signalsInterface.getName() + ")";
                if ("hashCode".equals(name))
                    return System.identityHashCode(proxy);
                if ("equals".equals(name))
                    return proxy == args[0];
            }
            String signature = signatureCache.get(method);
            // Signatures are computed at bind-time, so a signature miss here is an error
            if (signature == null)
                throw new IllegalStateException("Missing signal signature cache entry for method: " + method);
            qtObject.emitSignal(signature, args == null ? new Object[0] : args);
            return null; // Signal return values are void
        };
        return (T) Proxy.newProxyInstance(
                signalsInterface.getClassLoader(), new Class<?>[]{signalsInterface}, handler);
    }
}
