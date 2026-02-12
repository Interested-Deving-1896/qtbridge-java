/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

import org.jetbrains.annotations.NotNull;
import org.qtproject.qt.bridge.utils.ObjectToMapConverter;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * A Java-side property that bridges a single value to Qt/QML.
 *
 * <p>{@code QtProperty<T>} lets you bridge a mutable value to QML.
 * Changes can originate from either side (Java or QML), and observers on the Java
 * side get notified when the value changes.</p>
 *
 * <p>For bridging to work in QML, the property needs to be a member of a
 * {@link QMLRegistrable @QMLRegistrable} class so that QML can access it.</p>
 *
 * <h2 id="supported-types">Supported types</h2>
 * TODO verify the supported types for correctness.
 * <ul>
 *   <li><strong>Simple types</strong> (numbers, booleans, strings) are kept as-is.</li>
 *   <li><strong>Collections</strong> are copied defensively and stored as
 *       unmodifiable list copy to avoid accidental outside mutation.</li>
 *   <li><strong>Map&lt;String, Object&gt;</strong> types are supported.</li>
 *   <li><strong>Enum</strong> types have a limited support. They can be
 *   set in Java and read in QML, but cannot be written from QML
 *   <a href="https://bugreports.qt.io/browse/QTBUG-141710" >(QTBUG-141710)</a>. The Java enum
 *   is mapped to Map with members {@code name}, {@code ordinal}, and any additional member fields.
 *   These can be accessed at QML-side.</li>
 *   <li><strong>Plain Java arrays</strong> (e.g. {@code String[]}) are not supported at the
 *       moment and will cause {@link IllegalArgumentException}, see
 *       <a href="https://bugreports.qt.io/browse/QTBUG-140227" >(QTBUG-140227)</a></li>
 * </ul>
 *
 * <h2 id="subscription-semantics">Subscription semantics</h2>
 * <p>Observer registration methods (e.g. {@link #onValueChanged(Runnable)}) return an
 * {@link AutoCloseable} handle. Call {@link AutoCloseable#close()} to unsubscribe. If you
 * ignore the returned handle, the observer remains active until this object is disposed.
 * </p>
 *
 * <strong>Example</strong>
 * {@snippet file = "snippets/src/main/java/org/qtproject/qt/bridge/MySingleton.java"
 *           region = "qtproperty-declaration"
 *           lang = java}
 * <strong>Note:</strong> value change refers to the whole value changing. If the value is
 * for example a list, the whole list has changed and not just an individual value in the list.
 * For more granular updates see {@link QtListModel}.
 *
 * <h2 id="threading-notes">Threading notes</h2>
 * TODO check the threading correctness (<a href="https://bugreports.qt.io/browse/QTBUG-139177">QTBUG-139177</a>)
 * <p>Reads/writes are backed by an {@link AtomicReference}. Callbacks run on the thread that
 * triggers the change (Java setter) or the thread that delivers native notifications from Qt.
 * If your UI/state must be updated on a different thread, forward accordingly.</p>
 *
 * @param <T> property value type (after normalization, see <a href="#supported-types">Supported types</a>)
 */
public class QtProperty<T> {
    private final AtomicReference<T> valueRef = new AtomicReference<>();
    private static final Logger logger = Logger.getLogger("org.qtproject.qt.bridge");

    // Use CoW list to deal with concurrency. With this list it's acceptable to iterate
    // the list while modifying it. Using regular synchronized(observers) might be a bit
    // risky because we'd call user-code (observer.accept()) inside the synchronized block,
    // which could then deadlock if user calls e.g observe(). Performance-wise this should
    // be fine as the number of list traversals (every time a value changes) vastly outnumber
    // the list modifications (every time observe() is called).
    private final CopyOnWriteArrayList<QtPropertyObserver> observers = new CopyOnWriteArrayList<>();

    /**
     * Creates a property with an initial value.
     *
     * <p>The value is normalized (see <a href="#supported-types">Supported types</a>).
     * Arrays are not supported.</p>
     *
     * @param initialValue initial value (may be {@code null})
     * @throws IllegalArgumentException if {@code initialValue} is a plain array
     */
    public QtProperty(T initialValue) {
        valueRef.set(normalizeValue(initialValue));
    }

    /**
     * Returns the current value of this property.
     *
     * <p>The returned instance reflects the normalization rules described in
     * <a href="#supported-types">Supported types</a>.</p>
     *
     * @return the current value (may be {@code null})
     */
    public T getValue() {
        return valueRef.get(); // store immutable snapshot of the value
    }

    /**
     * Sets the value of this property.
     *
     * <p>The new value is normalized (see <a href="#supported-types">Supported types</a>).
     * If the normalized value equals the existing one (via {@link Objects#equals(Object, Object)}),
     * no notification is delivered.</p>
     *
     * @param newValue the new value (may be {@code null})
     * @throws IllegalArgumentException if {@code newValue} type is not supported
     */
    public void setValue(T newValue) {
        final T nextValue = normalizeValue(newValue);
        // Make atomic update and notify if value changed
        final T previousValue = valueRef.getAndSet(nextValue);
        if (!valuesAreEqual(nextValue, previousValue))
            notifyValueChanged();
    }

    @SuppressWarnings("unchecked") // About generics cast (T)
    private T normalizeValue(T value) {
        if (value == null)
            return null;
        if (value instanceof java.net.URI) {
            return value;
        }
        if (value.getClass().isAnnotationPresent(QMLRegistrable.class)) {
            // With QMLRegistrable use identity semantics (vs. value semantics):
            // store as-is, compare by reference
            return value;
        }
        if (value instanceof Collection<?> c) {
            // Defensive copy, and make unmodifiable to prevent modification outside of this class.
            // JSON arrays may have null values => make a copy that allows null elements
            // (List.copyOf() forbids nulls).
            java.util.ArrayList<Object> tmp = new java.util.ArrayList<>(c);
            return (T) java.util.Collections.unmodifiableList(tmp);
        }
        if (value instanceof Map<?, ?> m) {
            // Ensure QVariantMap compatibility: keys must be String. This is maybe a bit
            // heavy check to do every time, alternatively we could just document the
            // requirement and not check the key type.
            for (Object k : m.keySet()) {
                if (!(k instanceof String)) {
                    throw new IllegalArgumentException(
                            "Map keys must be String for QML/Qt (got key of type: " +
                                    (k == null ? "null" : k.getClass().getName()) + ")");
                }
            }
            // Defensive copy, and make unmodifiable to prevent modification outside of this class
            // Important: Map.copyOf() forbids null keys/values and would throw NPE for JSON "null"
            // Null is a valid JSON value => we need to preserve nulls, so do a LinkedHashMap
            // copy (to keep order) and wrap it.
            java.util.LinkedHashMap<String, Object> tmp = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<?, ?> e : m.entrySet()) {
                tmp.put((String) e.getKey(), e.getValue()); // value may be null
            }
            return (T) java.util.Collections.unmodifiableMap(tmp);
        }

        final Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            // Check if arrays component type (eg. 'int' if int[]) is supported,
            // and make a defensive copy if it is.
            final Class<?> component = valueClass.getComponentType();
            if (component.isPrimitive()
                || component == Integer.class || component == Double.class
                || component == Float.class || component == Byte.class
                || component == String.class || component == Character.class
                || component == Short.class || component == Long.class
                || component == Boolean.class)
            {
                int length = Array.getLength(value);
                Object copy = Array.newInstance(component, length);
                System.arraycopy(value, 0, copy, 0, length);
                return (T) copy;
            }
            throw new IllegalArgumentException("Unsupported array type " + valueClass + " " + component);
        }

        if (ObjectToMapConverter.isSimpleType(value))
            return value;

        // Type is currently not supported
        throw new IllegalArgumentException("Unsupported value type " + value.getClass().getName());
    }

    // Returns true if values are considered equal, and false otherwise
    private static boolean valuesAreEqual(Object x, Object y) {
        if (x == y)
            return true;
        // If only one of them is null they are not equal (if both are null, 'x == y' is true)
        if (x == null || y == null)
            return false;

        final Class<?> xClass = x.getClass();
        final Class<?> yClass = y.getClass();
        final boolean xIsArray = xClass.isArray();
        final boolean yIsArray = yClass.isArray();

        // Check if (only) one of them is array
        if (xIsArray != yIsArray)
            return false;

        // If values are not arrays -> do a more generic equality comparison
        if (!xIsArray)
            return Objects.equals(x, y);

        // Values are arrays, check if array types match
        if (xClass != yClass)
            return false;

        // Array types match -> cast and compare per element
        if (x instanceof int[] xx)
            return Arrays.equals(xx, (int[]) y);
        if (x instanceof long[] xx)
            return Arrays.equals(xx, (long[]) y);
        if (x instanceof short[] xx)
            return Arrays.equals(xx, (short[]) y);
        if (x instanceof byte[] xx)
            return Arrays.equals(xx, (byte[]) y);
        if (x instanceof char[] xx)
            return Arrays.equals(xx, (char[]) y);
        if (x instanceof boolean[] xx)
            return Arrays.equals(xx, (boolean[]) y);
        if (x instanceof float[] xx)
            return Arrays.equals(xx, (float[]) y);
        if (x instanceof double[] xx)
            return Arrays.equals(xx, (double[]) y);

        if (x instanceof Integer[] xx)
            return Arrays.equals(xx, (Integer[]) y);
        if (x instanceof Long[] xx)
            return Arrays.equals(xx, (Long[]) y);
        if (x instanceof Short[] xx)
            return Arrays.equals(xx, (Short[]) y);
        if (x instanceof Byte[] xx)
            return Arrays.equals(xx, (Byte[]) y);
        if (x instanceof Character[] xx)
            return Arrays.equals(xx, (Character[]) y);
        if (x instanceof Boolean[] xx)
            return Arrays.equals(xx, (Boolean[]) y);
        if (x instanceof Float[] xx)
            return Arrays.equals(xx, (Float[]) y);
        if (x instanceof Double[] xx)
            return Arrays.equals(xx, (Double[]) y);
        if (x instanceof String[] xx)
            return Arrays.equals(xx, (String[]) y);

        // This shouldn't happen as we check the component types in normalizeValue()
        logger.severe("QtProperty: Unsupported array type: " + xClass);
        return false;
    }

    /**
     * Registers a callback that runs whenever the value changes.
     *
     * <p>Returns an {@link AutoCloseable} you can call to unsubscribe. If you ignore the handle,
     * the observer remains active until this QtProperty is disposed.</p>
     *
     * <strong>Example</strong>
     * {@snippet file = "snippets/src/main/java/org/qtproject/qt/bridge/MySingleton.java"
     *           region = "qtproperty-valueChanged-with-sub"
     *           lang = java}
     *
     * @param consumer callback to run when the value changes; must not be {@code null}
     * @return an {@link AutoCloseable} that removes the observer when closed
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    public AutoCloseable onValueChanged(@NotNull Runnable consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        QtPropertyObserver observer = QtPropertyObserver.valueChanged(consumer);
        observe(observer);
        return () -> removeObserver(observer); // Returned AutoCloseable's close() calls removeObserver()
    }

    private void notifyValueChanged() {
        for (var o : observers) try { o.onValueChanged(); } catch (Throwable t) { t.printStackTrace(); }
    }

    /**
     * Adds a low-level observer that receives all property change callbacks.
     *
     * <p>For convenience, prefer {@link #onValueChanged(Runnable)}.</p>
     *
     * @param observer observer to add; must not be {@code null}
     * @throws NullPointerException if {@code observer} is {@code null}
     */
    public void observe(@NotNull QtPropertyObserver observer) {
        Objects.requireNonNull(observer, "observer must not be null");
        observers.addIfAbsent(observer);
    }

    /**
     * Removes a previously added observer.
     *
     * @param observer observer to remove; must not be {@code null}
     * @throws NullPointerException if {@code observer} is {@code null}
     */
    public void removeObserver(@NotNull QtPropertyObserver observer) {
        Objects.requireNonNull(observer, "observer must not be null");
        observers.remove(observer);
    }

    /**
     * A convenience method for mapping a plain old Java object fields
     * to a Map&lt;String, Object&gt;. The returned map can be used as a
     * QtProperty value. Object member fields and bean-like properties are
     * handled recursively.
     *
     * @param object whose fields to map.
     * @param includeNonPublic whether to include also private and protected members
     * @return the object whose fields are mapped to a Map&lt;String, Object&gt;
     */
    public static Map<String,Object> toMap(Object object, boolean includeNonPublic) {
        return ObjectToMapConverter.makeMap(object, includeNonPublic);
    }

    /**
     * Shorthand for {@link #toMap(Object, boolean) toMap(object, false)}.
     *
     * @param object object to map
     * @return a Map view of the object's fields
     * @see #toMap(Object, boolean)
     */
    public static Map<String,Object> toMap(Object object) {
        return toMap(object, false);
    }
}
