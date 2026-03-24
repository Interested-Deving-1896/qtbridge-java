/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
* Marks a method of a {@link QMLRegistrable} class as a QML signal.
*
* <p>The method must be {@code abstract} or {@code open} (non-final), and must
* return {@code void}.</p>
*
* @see QMLRegistrable
* @see QMLSignals
*/
@Documented
@Retention(SOURCE)
@Target(METHOD)
public @interface QMLSignal {}
