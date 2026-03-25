/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *  Marks a field that holds the set of QML signals for {@link QMLRegistrable} class.
 *
 * <p>The bridge inspects fields annotated with `@QMLSignals` to discover and expose
 * signal endpoints to QML, and to enable emitting those signals from Java/Kotlin.</p>
 *
 * <h2>Example</h2>
 * {@snippet file = "snippets/src/main/java/org/qtproject/qt/bridge/MyType.java"
 *           region = "qmlsignals-usage"
 *           lang = java}
 *
 * @see QMLRegistrable
 * @see QMLSignal
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface QMLSignals {}
