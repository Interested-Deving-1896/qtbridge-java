/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

public interface BackendCallback {
    void call();
    void callWithInt(int a);
    void callWithInteger(Integer a);
    void callWithBoolean(boolean b);
    void callWithBooleanObject(Boolean b);
    void callWithDouble(double d);
    void callWithDoubleObject(Double d);
    void callWithLong(long l);
    void callWithLongObject(Long l);
    void callWithFloat(float f);
    void callWithFloatObject(Float f);
    void callWithChar(char c);
    void callWithCharObject(Character c);

    void callWithTwoInts(int a, int b);
    void callWithThreeInts(int a, int b, int c);
    void callWithIntAndString(int num, String str);
    void callWithBooleanAndDouble(boolean flag, double value);

    // Object types
    void callWithString(String str);
    void callWithList(java.util.List<String> list);
    void callWithMap(java.util.Map<String, Object> map);
    void callWithUrl(java.net.URI url);
    void callWithEnum(SingletonTestBackend.Color color);
}
