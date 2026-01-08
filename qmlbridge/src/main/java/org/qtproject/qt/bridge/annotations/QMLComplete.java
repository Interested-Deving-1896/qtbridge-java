/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method as the QML element creation completion handler.
 *
 * <p>This annotation is used to mark the QML element completion handler. It is invoked
 * once when all QtProperties and QML children have been set, and QML considers the class
 * fully initialized.
 *
 * Catching the completion can be useful if your class needs to do expensive initialization work
 * that relies on several QtProperties; in these cases it might be preferable to wait until all
 * property values have been set first.</p>
 *
 * <h2>Example</h2>
 * {@snippet file = "snippets/src/main/java/org/qtproject/qt/bridge/MyType.java"
 *           region = "qml-complete"
 *           lang = java}
 *
 * <p>The handler is called on the Qt thread. It must not take parameters, and it must not return
 * a value. There can be at most one such handler per class. Handler is not invoked with singleton types.</p>
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface QMLComplete {}
