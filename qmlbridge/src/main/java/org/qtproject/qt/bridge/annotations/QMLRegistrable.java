/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a class to be registered as a QML-exposed type by the Java–QML bridge.
 *
 * <p>Apply this to classes you want to make visible to QML (either as a creatable type
 * or as a singleton).</p>
 *
 * <h2>Example</h2>
 * {@snippet file = "minimalapp/src/main/java/org/qtproject/qt/bridge/FruitBasket.java"
 *           region = "qmlregistrable-singleton"
 *           lang = java}
 *
 * @see QMLSignals
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface QMLRegistrable {
    /**
     * <p>Optional QML type name (empty means that class name is used).</p>
     * @return the QML type name to override.
     */
    String name() default "";

    /**
     * <p>QML module/URI to register into (empty means {@code "QtBridge"} is used).</p>
     *
     * @return the QML module name to override.
     */
    String module() default "QtBridge";

    /**
     * <p>If {@code true}, registers as a QML singleton; otherwise a creatable type.</p>
     *
     * @return whether the type is a singleton or not (creatable).
     */
    boolean singleton() default false;
}
