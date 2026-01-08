/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.jetbrains.annotations.NotNull;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;

import java.util.List;
import java.util.Map;
import java.net.URI;

@QMLRegistrable(singleton = true)
public class OverloadTest {
    public int invokePrimitive(int a, int b) {
        return a + b;
    }

    public double invokePrimitive(double a, double b) {
        return a + b;
    }

    public boolean invokePrimitive(boolean a, boolean b) {
        return a && b;
    }

    public char invokePrimitive(char a, char b) {
        return (char) (a + b);
    }

    public long invokePrimitive(long a, long b) {
        return a + b;
    }

    public float invokePrimitive(float a, float b) {
        return a + b;
    }

    public byte invokePrimitive(byte a, byte b) {
        return (byte) (a + b);
    }

    public short invokePrimitive(short a, short b) {
        return (short) (a + b);
    }

    public double invokePrimitive(int a, double b) {
        return a + b;
    }

    public double invokePrimitive(double a, int b) {
        return a + b;
    }

    public String invokePrimitive(String s, int a) {
        return s + a;
    }

    public String invokePrimitive(String s, double a) {
        return s + a;
    }

    public Double invokeBoxed(Integer s, Double a) {
        return s + a;
    }

    public String invokeBoxed(String s, Integer a) {
        return s + a;
    }

    public String invokeBoxed(String s, Double a) {
        return s + a;
    }

    public Boolean invokeBoxed(Boolean a, Boolean b) {
        return a && b;
    }

    public int invokeList(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).sum();
    }

    public double invokeListDouble(List<Double> list) {
        return list.stream().mapToDouble(Double::doubleValue).sum();
    }

    public String invokeListString(List<String> list) {
        return String.join(", ", list);
    }

    public <T> String invokeListGeneric(List<T> list) {
        return "List: " + list.toString();
    }

    public int sumFlatMapValues(@NotNull Map<String, Integer> map) {
        int sum = 0;
        for (Integer value : map.values())
            sum += value;
        return sum;
    }

    public Map<String, Object> echoMap(@NotNull Map<String, Object> map) {
        return map;
    }

    public URI echoUrl(@NotNull URI uri) {
        return uri;
    }
}
