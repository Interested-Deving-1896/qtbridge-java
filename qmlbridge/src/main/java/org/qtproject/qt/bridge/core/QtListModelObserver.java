/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Observer for {@link QtListModel} change notifications.
 *
 * <p>Implementations of this interface can be registered with
 * {@link QtListModel#observe(QtListModelObserver)}. For most use cases you don’t need
 * to implement this interface yourself — prefer the model’s convenience helpers
 * ({@link QtListModel#onInserted(TwoIntsConsumer)}, {@link QtListModel#onRemoved(TwoIntsConsumer)},
 * {@link QtListModel#onChanged(TwoIntsConsumer)}, {@link QtListModel#onReset(Runnable)},
 * {@link QtListModel#onSizeChanged(Runnable)}) which return {@link AutoCloseable} handles.</p>
 *
 * <p>For the range-based events ({@code inserted}, {@code removed}, {@code changed}),
 * parameters are {@code (first, count)}, where {@code first} is the zero-based index of the
 * first affected row and {@code count} is the number of consecutive rows affected.</p>
 *
 * @see QtListModel
 */
public interface QtListModelObserver {
    /**
     * Invoked after contiguous rows have been inserted.
     *
     * @param first zero-based index of the first inserted row
     * @param count number of rows inserted (≥ 1)
     */
    default void onInserted(int first, int count) {}
    /**
     * Invoked after contiguous rows have been removed.
     *
     * @param first zero-based index of the first removed row (position before removal)
     * @param count number of rows removed (≥ 1)
     */
    default void onRemoved(int first, int count) {}
    /**
     * Invoked after in-place data changes affecting existing rows.
     *
     * @param first zero-based index of the first changed row
     * @param count number of consecutive rows that changed (≥ 1)
     */
    default void onChanged(int first, int count) {}
    /**
     * Invoked after the model content was reset (replaced or cleared).
     */
    default void onReset() {}
    /**
     * Invoked whenever the model's item count changes.
     */
    default void onSizeChanged() {}

    /**
     * Functional interface used by range-based events.
     *
     * <p>Parameters are {@code (first, count)} where {@code first} is the starting index
     * and {@code count} the number of contiguous rows affected.</p>
     */
    @FunctionalInterface interface TwoIntsConsumer {
        /**
         * Handles a range-based event.
         * @param first zero-based start index
         * @param count number of rows affected (≥ 1)
         */
        void accept(int first, int count);
    }

    /**
     * Creates an observer that runs the given {@code consumer} whenever a row is inserted.
     *
     * @param consumer callback to invoke on insert events; must not be {@code null}
     * @return an observer that forwards insert notifications
     */
    static QtListModelObserver inserted(@NotNull TwoIntsConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return new QtListModelObserver() {
            @Override
            public void onInserted(int start, int count) {
                consumer.accept(start, count);
            }
        };
    }

    /**
     * Creates an observer that runs the given {@code consumer} whenever a row is removed.
     *
     * @param consumer callback to invoke on remove events; must not be {@code null}
     * @return an observer that forwards remove notifications
     */
    static QtListModelObserver removed(@NotNull TwoIntsConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return new QtListModelObserver() {
            @Override
            public void onRemoved(int start, int count) {
                consumer.accept(start, count);
            }
        };
    }

    /**
     * Creates an observer that runs the given {@code consumer} whenever row value changes.
     *
     * @param consumer callback to invoke on change events; must not be {@code null}
     * @return an observer that forwards change notifications
     */
    static QtListModelObserver changed(@NotNull TwoIntsConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return new QtListModelObserver() {
            @Override
            public void onChanged(int start, int count) {
                consumer.accept(start, count);
            }
        };
    }

    /**
     * Creates an observer that runs the given {@code consumer} whenever model is reset.
     *
     * @param consumer callback to invoke on reset events; must not be {@code null}
     * @return an observer that forwards reset notifications
     */
    static QtListModelObserver reset(@NotNull Runnable consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return new QtListModelObserver() {
            @Override
            public void onReset() {
                consumer.run(); }
        };
    }

    /**
     * Creates an observer that runs the given {@code consumer} whenever model size changes.
     *
     * @param consumer callback to invoke on size change events; must not be {@code null}
     * @return an observer that forwards size change notifications
     */
    static QtListModelObserver sizeChanged(@NotNull Runnable consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return new QtListModelObserver() {
            @Override
            public void onSizeChanged() {
                consumer.run(); }
        };
    }
}
