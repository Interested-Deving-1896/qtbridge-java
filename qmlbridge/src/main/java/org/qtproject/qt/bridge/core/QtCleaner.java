/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

import java.lang.ref.Cleaner;
import java.util.function.LongConsumer;

// QtCleaner is a shared cleaner class for classes that need to make sure native
// resources are released when Java-side becomes unreachable (phantom reference).
// It's shared because each cleaner spins a thread => save resources.
final class QtCleaner {
    private static final Cleaner CLEANER = Cleaner.create();
    private QtCleaner() {} // Prevent instantiation
    // Registers a cleanup that will call 'disposer' function when 'obj' becomes unreachable
    // The disposer function must accept the native handle (long)
    static Cleaner.Cleanable registerNativeCleanup(
            Object obj, long handle, LongConsumer disposer) {
        return CLEANER.register(obj, () -> disposer.accept(handle));
    }
}

