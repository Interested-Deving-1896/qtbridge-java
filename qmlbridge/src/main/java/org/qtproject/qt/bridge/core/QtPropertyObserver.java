/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * Observer for {@link QtProperty} value changes.
 *
 * <p>Implementations of this interface can be registered with
 * {@link QtProperty#observe(QtPropertyObserver)} or created via the convenience
 * factory {@link #valueChanged(Runnable)}. For most use cases you don’t need to
 * implement this interface yourself — use
 * {@link QtProperty#onValueChanged(Runnable)} which returns an {@link AutoCloseable}
 * handle you can store and later close to unsubscribe.</p>
 *
 * @see QtProperty
 */
public interface QtPropertyObserver {
    /**
     * Invoked when the associated {@link QtProperty} value has changed.
     */
    default void onValueChanged() {}

    /**
     * Creates an observer that runs the given {@code consumer} whenever the value changes.
     *
     * @param consumer callback to invoke when the value changes; must not be {@code null}
     * @return a {@code QtPropertyObserver} that forwards to {@code consumer}
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    static QtPropertyObserver valueChanged(@NotNull Runnable consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return new QtPropertyObserver() {
            @Override
            public void onValueChanged() {
                consumer.run();
            }
        };
    }
}
