/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

/**
 * Minimal public interface for emitting Qt signals from generated bridge code,
 * allowing generated QtImpl classes to live in the user's package.
*/
public interface QtSignalEmitter {
    void emitSignal(String javaSignature, Object... args);
}
