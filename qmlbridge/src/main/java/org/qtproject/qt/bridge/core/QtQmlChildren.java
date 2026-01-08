/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A small utility class for retrieving the QML child elements.
 *
 * <p>QML elements may contain other QML elements, and it's sometimes useful
 * to know these child elements.</p>
 *
 * <h2>Example</h2>
 * {@snippet file = "snippets/src/main/java/org/qtproject/qt/bridge/MyType.java"
 *           region = "qml-children"
 *           lang = java}
 */
public final class QtQmlChildren {
    private QtQmlChildren() {}

    /**
     * Returns a list of all @QMLRegistrable children.
     *
     * @param parent parent item whose children are queried
     * @return list of children
     */
    public static List<Object> children(@NotNull Object parent) {
        return children(parent, null);
    }

    /**
     * Returns a list of @QMLRegistrable children who are of class {@code klass}.
     *
     * @param parent parent item whose children are queried
     * @param klass filter - return only (sub)classes of this class
     * @return list of children
     * @param <T> The class type used as filter
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> children(@NotNull Object parent, Class<T> klass) {
        Objects.requireNonNull(parent);
        return (List<T>)nativeQmlChildren(parent, klass);
    }

    private static native List<Object> nativeQmlChildren(Object parent, Class<?> klass);
}
