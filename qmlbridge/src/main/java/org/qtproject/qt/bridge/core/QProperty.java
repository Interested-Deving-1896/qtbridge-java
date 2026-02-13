/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

record QProperty(
        String name,
        String javaType,
        String cppType,
        boolean writeable,
        boolean readable,
        boolean constant,
        String notificationSignal,
        boolean isPrimitive,
        byte shape,
        byte type) {}
