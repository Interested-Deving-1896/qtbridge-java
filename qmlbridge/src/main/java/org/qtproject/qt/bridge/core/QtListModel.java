/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

import org.jetbrains.annotations.NotNull;
import org.qtproject.qt.bridge.utils.ObjectToMapConverter;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.utils.loader.NativeLibraryLoader;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;
import java.lang.ref.Cleaner;
import java.util.List;
import java.util.Map;

/**
 * A Java-side list model that bridges a list of items to a Qt/QML model.
 *
 * <p>Instances of {@code QtListModel<T>} let you bridge Java/Kotlin list data to QML item views. Model data is
 * editable on both Java- and QML side. While it's possible to use {@link QtProperty} to bridge lists to QML too,
 * QtListModel can provide better performance when used in QML views, or when partial list updates are possible.</p>
 *
 * <p>For bridging to work, QtListModel must be a member variable of a {@link QMLRegistrable @QMLRegistrable} class.
 * For example let's assume:
 * {@snippet file = "snippets/src/main/java/org/qtproject/qt/bridge/MySingleton.java"
 *           region = "qmlregistrable-singleton"
 *           lang = java}
 * Then have it as a member variable:
 * {@snippet file = "snippets/src/main/java/org/qtproject/qt/bridge/MySingleton.java"
 *           region = "qtlistmodel-declaration"
 *           lang = java}
 * </p>
 *
 * <h2 id="supported-types">Supported types</h2>
 * TODO add supported types
 *
 * <h2 id="subscription-semantics">Subscription semantics</h2>
 * <p>Observer registration methods such as {@link #onSizeChanged(Runnable)} and {@link #onReset(Runnable)}
 * return an {@link AutoCloseable} handle. Call {@link AutoCloseable#close()} to unsubscribe.
 * If you ignore the returned handle, the observer remains active until this model is disposed
 * via {@link #dispose()}.
 * <strong>Example</strong>
 * {@snippet file = "snippets/src/main/java/org/qtproject/qt/bridge/MySingleton.java"
 *           region = "qtlistmodel-sizeChanged-with-sub"
 *           lang = java}
 * </p>
 *
 * <h2 id="threading-notes">Threading notes</h2>
 * TODO confirm the threading behaviour
 * <p> At Java-side values can be updated from any thread.
 * Change notifications are delivered on the thread that {@link QtQuickApplication} lives in.
 * If your application must be updated on another thread, forward accordingly.</p>
 *
 * @param <T> Item type.
 */
public class QtListModel<T>  {
    static {
        NativeLibraryLoader.loadLibrary();
    }
    private final CopyOnWriteArrayList<QtListModelObserver> observers = new CopyOnWriteArrayList<>();
    private final long nativeHandle;
    private Cleaner.Cleanable cleanable = null;

    /**
     * Creates an empty model.
     */
    public QtListModel() {
        nativeHandle = nativeCreate();
        cleanable = QtCleaner.registerNativeCleanup(this, nativeHandle, QtListModel::nativeDispose);
    }

    /**
     * Creates a model pre-populated with the given items.
     *
     * <p>See also <a href="#supported-types">Supported types.</a></p>
     *
     * @param items initial items; must not be {@code null}.
     * @throws NullPointerException if {@code items} is {@code null}
     */
    public QtListModel(@NotNull List<T> items) {
        Objects.requireNonNull(items, "items must not be null");
        nativeHandle = nativeCreate();
        cleanable = QtCleaner.registerNativeCleanup(this, nativeHandle, QtListModel::nativeDispose);
        setItems(items);
    }

    /**
     * Replaces the entire content of the model with {@code items}.
     *
     * <p>If the model was non-empty, {@link #onReset(Runnable)} and
     * {@link #onSizeChanged(Runnable)} will be emitted.</p>
     *
     * <p>See also <a href="#supported-types">Supported types.</a></p>
     *
     * @param items new content; must not be {@code null}
     * @throws NullPointerException if {@code items} is {@code null}
     */
    public void setItems(@NotNull List<T> items) {
        Objects.requireNonNull(items, "items must not be null");
        ArrayList<Map<String, Object>> allItems = new ArrayList<>(items.size());
        for (T item : items)
            allItems.add(ObjectToMapConverter.makeMap(item));
        nativeReplaceAll(nativeHandle, allItems);
    }

    /**
     * Releases observers and native resources owned by this model.
     *
     * <p>After calling {@code dispose()}, no further operations should be performed
     * on this instance.</p>
     */
    public void dispose() {
        observers.clear();
        cleanable.clean();
    }

    /**
     * Returns whether the model currently contains an item equal to {@code item}.
     *
     * <p>Equality is based on the internal native representation match.</p>
     *
     * @param item item to match; must not be {@code null}
     * @return {@code true} if an equal item exists, otherwise {@code false}
     * @throws NullPointerException if {@code item} is {@code null}
     */
    public boolean contains(@NotNull T item) {
        Objects.requireNonNull(item, "item must not be null");
        Map<String, Object> itemMap = ObjectToMapConverter.makeMap(item);
        return nativeContains(nativeHandle, itemMap);
    }

    /**
     * Returns the number of items currently held by the model.
     *
     * @return current item count
     */
    public int size() {
        return nativeSize(nativeHandle);
    }

    /**
     * Appends a single item to the end of the model.
     *
     * <p>Notifies {@link #onInserted(QtListModelObserver.TwoIntsConsumer)} and
     * {@link #onSizeChanged(Runnable)} observers.</p>
     *
     * <p>See also <a href="#supported-types">Supported types.</a></p>
     *
     * @param item item to append; must not be {@code null}
     * @throws NullPointerException if {@code item} is {@code null}
     */
    public void appendItem(@NotNull T item) {
        Objects.requireNonNull(item, "item must not be null");
        nativeAppendItem(nativeHandle, ObjectToMapConverter.makeMap(item));
    }

    /**
     * Replaces the item at {@code index} with {@code item}.
     *
     * <p>Notifies {@link #onChanged(QtListModelObserver.TwoIntsConsumer)} observers.
     * The index must satisfy {@code 0 <= index < size()}.</p>
     *
     * @param index zero-based index
     * @param item  replacement item; must not be {@code null}
     * @throws NullPointerException if {@code item} is {@code null}
     */
    public void updateItemAt(int index, @NotNull T item) {
        Objects.requireNonNull(item, "item must not be null");
        nativeUpdateItemAt(nativeHandle, index, ObjectToMapConverter.makeMap(item));
    }

    /**
     * Removes the item at {@code index}.
     *
     * <p>Notifies {@link #onRemoved(QtListModelObserver.TwoIntsConsumer)} and
     * {@link #onSizeChanged(Runnable)} observers.</p>
     *
     * @param index zero-based index; must satisfy {@code 0 <= index < size()}
     */
    public void removeItemAt(int index) {
        nativeRemoveItemAt(nativeHandle, index);
    }

    /**
     * Removes the first occurrence of an item equal to {@code item}.
     *
     * <p>Notifies {@link #onRemoved(QtListModelObserver.TwoIntsConsumer)} and
     * {@link #onSizeChanged(Runnable)} observers.</p>
     *
     * @param item item to remove; must not be {@code null}
     * @throws NullPointerException if {@code item} is {@code null}
     */
    public void removeItem(@NotNull T item) {
        Objects.requireNonNull(item, "item must not be null");
        nativeRemoveItem(nativeHandle, ObjectToMapConverter.makeMap(item));
    }

    /**
     * Clears the model.
     *
     * <p>Notifies {@link #onReset(Runnable)} and {@link #onSizeChanged(Runnable)} observers.</p>
     */
    public void reset() {
        nativeReset(nativeHandle);
    }

    /**
     * Registers a low-level observer that receives all model change callbacks.
     *
     * <p>For convenience, prefer {@link #onInserted(QtListModelObserver.TwoIntsConsumer)},
     * {@link #onRemoved(QtListModelObserver.TwoIntsConsumer)}, {@link #onChanged(QtListModelObserver.TwoIntsConsumer)},
     * {@link #onSizeChanged(Runnable)}, or {@link #onReset(Runnable)}.</p>
     *
     * <p>See <a href="#subscription-semantics">Subscription semantics</a> for lifecycle notes.</p>
     * <p>See <a href="#threading-notes">Threading notes</a> for threading considerations.</p>
     *
     * @param observer observer to add; must not be {@code null}
     * @throws NullPointerException if {@code item} is {@code null}
     */
    public void observe(@NotNull QtListModelObserver observer) {
        Objects.requireNonNull(observer, "observer must not be null");
        observers.addIfAbsent(observer);
    }

    /**
     * Unregisters a previously added observer.
     *
     * <p>See <a href="#subscription-semantics">Subscription semantics</a> for lifecycle notes.</p>
     * <p>See <a href="#threading-notes">Threading notes</a> for threading considerations.</p>
     *
     * @param observer observer to remove; must not be {@code null}
     * @throws NullPointerException if {@code item} is {@code null}
     */
    public void removeObserver(@NotNull QtListModelObserver observer) {
        Objects.requireNonNull(observer, "observer must not be null");
        observers.remove(observer);
    }

    /**
     * Subscribes to row insertion events.
     *
     * <p>Invoked with {@code (first, count)} whenever contiguous rows are inserted.</p>
     *
     * <p><strong>Example</strong></p>
     * {@snippet file="snippets/src/main/java/org/qtproject/qt/bridge/MySingleton.java"
     *           region="qtlistmodel-onInserted"
     *           lang=java }
     *
     * <p>See <a href="#subscription-semantics">Subscription semantics</a> for lifecycle notes.</p>
     * <p>See <a href="#threading-notes">Threading notes</a> for threading considerations.</p>
     *
     * @param consumer callback; must not be {@code null}
     * @return an {@link AutoCloseable} that removes the observer when closed
     * @see #onRemoved(QtListModelObserver.TwoIntsConsumer)
     * @see #onChanged(QtListModelObserver.TwoIntsConsumer)
     * @see #onSizeChanged(Runnable)
     * @see #onReset(Runnable)
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    public AutoCloseable onInserted(@NotNull QtListModelObserver.TwoIntsConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        QtListModelObserver observer = QtListModelObserver.inserted(consumer);
        observe(observer);
        return () -> removeObserver(observer);
    }

    /**
     * Subscribes to row removal events.
     *
     * <p>Invoked with {@code (first, count)} whenever contiguous rows are removed.</p>
     *
     * <p><strong>Example</strong></p>
     * {@snippet file="snippets/src/main/java/org/qtproject/qt/bridge/MySingleton.java"
     *           region="qtlistmodel-onRemoved"
     *           lang=java }
     *
     * <p>See <a href="#subscription-semantics">Subscription semantics</a> for lifecycle notes.</p>
     * <p>See <a href="#threading-notes">Threading notes</a> for threading considerations.</p>
     *
     * @param consumer callback; must not be {@code null}
     * @return an {@link AutoCloseable} that removes the observer when closed
     * @see #onInserted(QtListModelObserver.TwoIntsConsumer)
     * @see #onChanged(QtListModelObserver.TwoIntsConsumer)
     * @see #onSizeChanged(Runnable)
     * @see #onReset(Runnable)
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    public AutoCloseable onRemoved (@NotNull QtListModelObserver.TwoIntsConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        QtListModelObserver observer = QtListModelObserver.removed(consumer);
        observe(observer);
        return () -> removeObserver(observer);
    }

    /**
     * Subscribes to in-place data change events.
     *
     * <p>Invoked with {@code (first, count)} when values of existing rows change without
     * altering the size of the model.</p>
     *
     * <p><strong>Example</strong></p>
     * {@snippet file="snippets/src/main/java/org/qtproject/qt/bridge/MySingleton.java"
     *           region="qtlistmodel-onChanged"
     *           lang=java }
     *
     * <p>See <a href="#subscription-semantics">Subscription semantics</a> for lifecycle notes.</p>
     * <p>See <a href="#threading-notes">Threading notes</a> for threading considerations.</p>
     *
     * @param consumer callback; must not be {@code null}
     * @return an {@link AutoCloseable} that removes the observer when closed
     * @see #onInserted(QtListModelObserver.TwoIntsConsumer)
     * @see #onRemoved(QtListModelObserver.TwoIntsConsumer)
     * @see #onSizeChanged(Runnable)
     * @see #onReset(Runnable)
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    public AutoCloseable onChanged (@NotNull QtListModelObserver.TwoIntsConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        QtListModelObserver observer = QtListModelObserver.changed(consumer);
        observe(observer);
        return () -> removeObserver(observer);
    }

    /**
     * Subscribes a callback that runs whenever the model's <em>size</em> changes.
     *
     * <p>Fired after operations that change the number of items, such as
     * {@link #appendItem(Object)}. It is <strong>not</strong> fired for
     * in-place edits like {@link #updateItemAt(int, Object)} and value changes from QML.</p>
     *
     * <p><strong>Example</strong></p>
     * {@snippet file="snippets/src/main/java/org/qtproject/qt/bridge/MySingleton.java"
     *           region="qtlistmodel-onSizeChanged"
     *           lang=java }
     *
     * <p>See <a href="#subscription-semantics">Subscription semantics</a> for lifecycle notes.</p>
     * <p>See <a href="#threading-notes">Threading notes</a> for threading considerations.</p>
     *
     * @param consumer callback to run when the item count changes; must not be {@code null}
     * @return an {@code AutoCloseable} that removes the observer when closed
     * @see #onInserted(QtListModelObserver.TwoIntsConsumer)
     * @see #onRemoved(QtListModelObserver.TwoIntsConsumer)
     * @see #onReset(Runnable)
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    public AutoCloseable onSizeChanged(@NotNull Runnable consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        QtListModelObserver observer = QtListModelObserver.sizeChanged(consumer);
        observe(observer);
        return () -> removeObserver(observer);
    }

    /**
     * Subscribes to reset events (the model content was replaced or cleared).
     *
     * <p><strong>Example</strong></p>
     * {@snippet file="snippets/src/main/java/org/qtproject/qt/bridge/MySingleton.java"
     *           region="qtlistmodel-onReset"
     *           lang=java }
     *
     * <p>See <a href="#subscription-semantics">Subscription semantics</a> for lifecycle notes.</p>
     * <p>See <a href="#threading-notes">Threading notes</a> for threading considerations.</p>
     *
     * @param consumer callback; must not be {@code null}
     * @return an {@link AutoCloseable} that removes the observer when closed
     * @see #onInserted(QtListModelObserver.TwoIntsConsumer)
     * @see #onRemoved(QtListModelObserver.TwoIntsConsumer)
     * @see #onChanged(QtListModelObserver.TwoIntsConsumer)
     * @see #onSizeChanged(Runnable)
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    public AutoCloseable onReset(@NotNull Runnable consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        QtListModelObserver observer = QtListModelObserver.reset(consumer);
        observe(observer);
        return () -> removeObserver(observer);
    }

    // When native model informs of changes, these notify the potential observers
    private void notifyInserted(int pos, int count) {
        for (var o : observers) try { o.onInserted(pos, count); } catch (Throwable t) { t.printStackTrace(); }
        notifySizeChanged();
    }
    private void notifyRemoved(int pos, int count) {
        for (var o : observers) try { o.onRemoved(pos, count); } catch (Throwable t) { t.printStackTrace(); }
        notifySizeChanged();
    }
    private void notifyChanged(int pos, int count) {
        for (var o : observers) try { o.onChanged(pos, count); } catch (Throwable t) { t.printStackTrace(); }
    }
    private void notifyReset() {
        for (var o : observers) try { o.onReset(); } catch (Throwable t) { t.printStackTrace(); }
        notifySizeChanged();
    }
    private void notifySizeChanged() {
        for (var o : observers) try { o.onSizeChanged(); } catch (Throwable t) { t.printStackTrace(); }
    }

    // Native callbacks (native model calls these to inform of changes)
    private void onRowsInsertedFromNative(int first, int last) {
        notifyInserted(first, last - first + 1);
    }
    private void onRowsRemovedFromNative(int first, int last) {
        notifyRemoved(first, last - first + 1);
    }
    private void onDataChangedFromNative(int first, int last)  {
        notifyChanged(first, last - first + 1);
    }
    private void onResetFromNative() {
        notifyReset();
    }

    // Native functions (Java-side calls these to update the model)
    private native long nativeCreate();
    private static native void nativeDispose(long handle);
    private native int nativeSize(long ptr);
    private native boolean nativeContains(long ptr, Map<String, Object> item);
    private native void nativeAppendItem(long ptr, Map<String, Object> item);
    private native void nativeUpdateItemAt(long ptr, int index, Map<String, Object> item);
    private native void nativeRemoveItem(long ptr, Map<String, Object> item);
    private native void nativeRemoveItemAt(long ptr, int index);
    private native void nativeReset(long ptr);
    private native void nativeReplaceAll(long ptr, List<Map<String,Object>> items);
}
