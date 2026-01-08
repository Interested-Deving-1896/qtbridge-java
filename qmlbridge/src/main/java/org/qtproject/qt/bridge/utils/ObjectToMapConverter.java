/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;


// Utility class for converting arbitrary Objects into Map<String, Object> representations.
public class ObjectToMapConverter {

    // Makes a Map<String, Object> of the provided item's properties.
    // 'includeNonPublic' is used to control whether non-public fields should be converted too.
    public static <T> Map<String, Object> makeMap(T item, boolean includeNonPublic) {
        return makeMap(item, new HashSet<>(), includeNonPublic);
    }

    public static <T> Map<String, Object> makeMap(T item) {
        return makeMap(item, new HashSet<>(), false);
    }

    private static <T> Map<String, Object> makeMap(T item, Set<Object> visited, boolean includeNonPublic) {
        if (item == null || visited.contains(item))
            return Collections.emptyMap();
        if (isSimpleType(item))
            return Collections.singletonMap("value", item);
        visited.add(item);
        try {
            return handleObject(item, visited, includeNonPublic);
        } finally {
            visited.remove(item);
        }
    }

    // Helper for decapitalizing strings when constructing properties from
    // getters. Attempts to keep acronyms (anything that has more than one
    // leading consecutive capital letter is considered an acronym).
    private static String decapitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        if (s.length() > 1 && Character.isUpperCase(s.charAt(0)) && Character.isUpperCase(s.charAt(1)))
            // Preserve acronyms like "URL" -> "URL"
            return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    // Returns a property name if the method looks like a getter:
    // - getX(): returns "x"
    // - isX(): returns "x" (if boolean/Boolean return type)
    // - returns null otherwise
    private static String propertyNameFromGetter(Method method) {
        if (method == null)
            return null;
        if (method.isSynthetic())
            return null;
        if (method.getParameterCount() != 0)
            return null;
        if (method.getReturnType() == void.class)
            return null;

        final String name = method.getName();
        if ("getClass".equals(name))
            return null; // skip Object.getClass

        // 'getX' getter
        if (name.startsWith("get") && name.length() > 3)
            return decapitalize(name.substring(3));

        // 'isX' boolean getter
        if (name.startsWith("is") && name.length() > 2) {
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class || returnType == Boolean.class)
                return decapitalize(name.substring(2));
        }
        return null;
    }

    public static boolean isSimpleType(Object obj) {
        if (obj == null)
            return true;
        return obj instanceof String ||
                obj instanceof Number ||
                obj instanceof Boolean ||
                obj instanceof Character ||
                obj instanceof URI ||
                obj.getClass().isEnum();
    }

    private static Map<String, Object> handleObject(
            Object obj, Set<Object> visited, boolean includeNonPublic) {
        Map<String, Object> result = new HashMap<>();
        // Pass 1: fields
        mapFields(obj, result, visited, includeNonPublic);
        // Pass 2: public getters ('bean-like')
        // In case of conflicts, the fields from pass 1 have higher priority.
        // This pass is used because kotlin data class properties are private
        // and not disovered when includeNonPublic = false (default). These are
        // only accessible through getter functions. We could also force the
        // introspection of private members, but then we could also get properties
        // that should not be used.
        mapGettersToProperties(obj, result, visited, includeNonPublic);
        return result;
    }

    // Maps the fields of given 'obj' to the result Map (recusively).
    // Processes the inheritance chain but stops before the top-most 'Object'.
    // Ignores static and synthetic fields
    private static void mapFields(Object obj, Map<String,Object> result,
                                  Set<Object> visited, boolean includeNonPublic) {
        Class<?> clazz = obj.getClass();
        // Traverse the class hierarchy up, but stop before processing the 'Object' class
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isSynthetic() || result.containsKey(field.getName()))
                    continue;
                int mods = field.getModifiers();
                if (Modifier.isStatic(mods))
                    continue;
                if (!Modifier.isPublic(mods) && !includeNonPublic)
                    continue;
                try {
                    field.setAccessible(true);
                    // Set the name and value to result map
                    if (!result.containsKey(field.getName()))
                        result.put(field.getName(), mapValue(field.get(obj), visited, includeNonPublic));
                } catch (Exception e) {
                    System.out.println("Failed to get value of " + field.getName() + ": " + e.getMessage());
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    // Returns the value of given 'value' object that can be placed as the
    // value field for a property.
    private static Object mapValue(Object value, Set<Object> visited, boolean includeNonPublic) {
        if (value == null)
            return null;
        if (isSimpleType(value))
            return value;

        // If value is an array, build and return a List whose values are mapped
        if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            List<Object> result = new ArrayList<>(length);
            for (int i = 0; i < length; ++i)
                result.add(mapValue(Array.get(value, i), visited, includeNonPublic));
            return result;
        }

        // If value is a List, build and return a List whose values are mapped
        if (value instanceof List) {
            List<?> sourceList = (List<?>) value;
            List<Object> result = new ArrayList<>(sourceList.size());
            for (Object entry : sourceList)
                result.add(mapValue(entry, visited, includeNonPublic));
            return result;
        }

        // If value is a Map, build and return a Map<String, Object> whose values are mapped
        if (value instanceof Map) {
            Map<?, ?> sourceMap = (Map<?, ?>) value;
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
                result.put(String.valueOf(entry.getKey()),
                        mapValue(entry.getValue(), visited, includeNonPublic));
            }
            return result;
        }
        // Since we are here, the 'value' is not not a simple type, list, array nor map
        // -> map it to another Map<String, Object>
        return makeMap(value, visited, includeNonPublic);
    }

    // Maps getter functions to properties (and values). Needed in particular for
    // Kotlin data classes whose properties themselves are private and are exposed
    // through getter functions instead
    private static void mapGettersToProperties(Object obj, Map<String,Object> result,
                                       Set<Object> visited, boolean includeNonPublic) {
        // Iterate through all public methods and see if some of them are getters
        for (Method method : obj.getClass().getMethods()) {
            String propertyName = propertyNameFromGetter(method);
            if (propertyName == null)
                continue; // not a getter (getX, isX)

            final String methodName = method.getName();
            final Class<?> returnType = method.getReturnType();
            final boolean isBooleanIsGetter =
                    methodName.startsWith("is") && methodName.length() > 2 &&
                            (returnType == boolean.class || returnType == Boolean.class);
            // For boolean getters 'isX()'', expose both:
            // - the property name ("x"), and
            // - an alias using the original method name ("isX").
            // This should help with Kotlin boolean properties that are actually named 'isX'
            final String isAlias = isBooleanIsGetter ? methodName : null;

            // Sanity-check property isn't set already
            if (result.containsKey(propertyName) && (isAlias == null || result.containsKey(isAlias)))
                continue;

            try {
                // Try to get the value by invoking the resolved getter. Then insert both
                // property and isAlias (as applicable)
                Object mapped = mapValue(method.invoke(obj), visited, includeNonPublic);
                if (!result.containsKey(propertyName))
                    result.put(propertyName, mapped);
                if (isAlias != null && !result.containsKey(isAlias))
                    result.put(isAlias, mapped);
            } catch (Exception e) {
                System.out.println("Failed to invoke getter " + method.getName() + ": " + e.getMessage());
            }
        }
    }
}
