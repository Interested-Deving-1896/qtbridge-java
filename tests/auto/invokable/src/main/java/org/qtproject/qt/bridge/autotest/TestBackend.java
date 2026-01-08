/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;

import java.util.ArrayList;
import java.util.List;

@QMLRegistrable(name = "TestBackend", singleton = true)
public class TestBackend {
    public Integer addInteger(Integer a, Integer b) {
        return a + b;
    }

    public int addInt(int a, int b) {
        return a + b;
    }

    public Long addLongObject(Long a, Long b) {
        return a + b;
    }

    public long addLong(long a, long b) {
        return a + b;
    }

    public Short addShortObject(Short a, Short b) {
        return (short) (a + b);
    }

    public short addShort(short a, short b) {
        return (short) (a + b);
    }

    public Byte addByteObject(Byte a, Byte b) {
        return (byte) (a + b);
    }

    public byte addByte(byte a, byte b) {
        return (byte) (a + b);
    }

    public Character addCharObject(Character a, Character b) {
        return (char) (a + b);
    }

    public char addChar(char a, char b) {
        return (char) (a + b);
    }

    public Boolean andBooleanObject(Boolean a, Boolean b) {
        return a && b;
    }

    public boolean andBoolean(boolean a, boolean b) {
        return a && b;
    }

    public Float addFloatObject(Float a, Float b) {
        return a + b;
    }

    public float addFloat(float a, float b) {
        return a + b;
    }

    public Double addDoubleObject(Double a, Double b) {
        return a + b;
    }

    public double addDouble(double a, double b) {
        return a + b;
    }

    public String concatString(String a, String b) {
        return a + b;
    }

    public List<String> concatList(List<String> a, List<String> b) {
        List<String> result = new ArrayList<>(a);
        result.addAll(b);
        return result;
    }

    public int nonAs€ciiöäEcho(int a) {
        return a;
    }
}
