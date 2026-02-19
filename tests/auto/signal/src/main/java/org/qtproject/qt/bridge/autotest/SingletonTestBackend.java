/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLSignals;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;

@QMLRegistrable(singleton = true)
public class SingletonTestBackend {
    public enum Color {
        RED("#FF0000"),
        GREEN("#00FF00"),
        BLUE("#0000FF"),;
        private final String rgb;
        Color(String rgb) {
            this.rgb = rgb;
        }
        public String getRgb() {
            return rgb;
        }
    }

    @QMLSignals
    BackendCallback backendCallback;
    public void triggerCall() {
        backendCallback.call();
    }
    public void triggerCallWithInt(int a) {
        backendCallback.callWithInt(a);
    }
    public void triggerCallWithInteger(Integer a) {
        backendCallback.callWithInteger(a);
    }
    public void triggerCallWithBoolean(boolean b) {
        backendCallback.callWithBoolean(b);
    }
    public void triggerCallWithBooleanObject(Boolean b) {
        backendCallback.callWithBooleanObject(b);
    }
    public void triggerCallWithDouble(double d) {
        backendCallback.callWithDouble(d);
    }
    public void triggerCallWithDoubleObject(Double d) {
        backendCallback.callWithDoubleObject(d);
    }
    public void triggerCallWithLong(long l) {
        backendCallback.callWithLong(l);
    }
    public void triggerCallWithLongObject(Long l) {
        backendCallback.callWithLongObject(l);
    }
    public void triggerCallWithFloat(float f) {
        backendCallback.callWithFloat(f);
    }
    public void triggerCallWithFloatObject(Float f) {
        backendCallback.callWithFloatObject(f);
    }
    public void triggerCallWithChar(char c) {
        backendCallback.callWithChar(c);
    }
    public void triggerCallWithCharObject(Character c) {
        backendCallback.callWithCharObject(c);
    }
    public void triggerCallWithTwoInts(int a, int b) {
        backendCallback.callWithTwoInts(a, b);
    }
    public void triggerCallWithThreeInts(int a, int b, int c) {
        backendCallback.callWithThreeInts(a, b, c);
    }
    public void triggerCallWithIntAndString(int num, String str) {
        backendCallback.callWithIntAndString(num, str);
    }
    public void triggerCallWithBooleanAndDouble(boolean flag, double value) { backendCallback.callWithBooleanAndDouble(flag, value); }
    // Object types
    public void triggerCallWithString(String str) {
        backendCallback.callWithString(str);
    }
    public void triggerCallWithList(java.util.List<String> list) {
        backendCallback.callWithList(list);
    }
    public void triggerCallWithMap(java.util.Map<String, Object> map) { backendCallback.callWithMap(map); }
    public void triggerCallWithUrl(java.net.URI url) { backendCallback.callWithUrl(url); }
    public void triggerCallWithEnum() { backendCallback.callWithEnum(Color.BLUE); }

    public String callbackProxyToString() {
        return backendCallback.toString();
    }
}
