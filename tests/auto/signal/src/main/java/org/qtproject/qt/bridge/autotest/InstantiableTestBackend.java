/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import org.qtproject.qt.bridge.annotations.QMLSignals;
import org.qtproject.qt.bridge.annotations.QMLSignal;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;

@QMLRegistrable(singleton = false)
public class InstantiableTestBackend {
    @QMLSignals
    BackendCallback backendCallback;

    // Same signals as in BackendCallback but declared individually
    @QMLSignal
    public void signalNoParams() {};
    @QMLSignal
    public void signalWithInt(int a) {};
    @QMLSignal
    public void signalWithInteger(Integer a) {};
    @QMLSignal
    public void signalWithBoolean(boolean b) {};
    @QMLSignal
    public void signalWithBooleanObject(Boolean b) {};
    @QMLSignal
    public void signalWithDouble(double d) {};
    @QMLSignal
    public void signalWithDoubleObject(Double d) {};
    @QMLSignal
    public void signalWithLong(long l) {};
    @QMLSignal
    public void signalWithLongObject(Long l) {};
    @QMLSignal
    public void signalWithFloat(float f) {};
    @QMLSignal
    public void signalWithFloatObject(Float f) {};
    @QMLSignal
    public void signalWithChar(char c) {};
    @QMLSignal
    public void signalWithCharObject(Character c) {};
    @QMLSignal
    public void signalWithTwoInts(int a, int b) {};
    @QMLSignal
    public void signalWithThreeInts(int a, int b, int c) {};
    @QMLSignal
    public void signalWithIntAndString(int num, String str) {};
    @QMLSignal
    public void signalWithBooleanAndDouble(boolean flag, double value) {};
    @QMLSignal
    public void signalWithString(String str) {};
    @QMLSignal
    public void signalWithList(java.util.List<String> list) {};
    @QMLSignal
    public void signalWithMap(java.util.Map<String, Object> map) {};
    @QMLSignal
    public void signalWithUrl(java.net.URI url) {};
    @QMLSignal
    public void signalWithEnum(SingletonTestBackend.Color color) {};

    public void triggerCall() {
        backendCallback.call();
        signalNoParams();
    }
    public void triggerCallWithInt(int a) {
        backendCallback.callWithInt(a);
        signalWithInt(a);
    }
    public void triggerCallWithInteger(Integer a) {
        backendCallback.callWithInteger(a);
        signalWithInteger(a);
    }
    public void triggerCallWithBoolean(boolean b) {
        backendCallback.callWithBoolean(b);
        signalWithBoolean(b);
    }
    public void triggerCallWithBooleanObject(Boolean b) {
        backendCallback.callWithBooleanObject(b);
        signalWithBooleanObject(b);
    }
    public void triggerCallWithDouble(double d) {
        backendCallback.callWithDouble(d);
        signalWithDouble(d);
    }
    public void triggerCallWithDoubleObject(Double d) {
        backendCallback.callWithDoubleObject(d);
        signalWithDoubleObject(d);
    }
    public void triggerCallWithLong(long l) {
        backendCallback.callWithLong(l);
        signalWithLong(l);
    }
    public void triggerCallWithLongObject(Long l) {
        backendCallback.callWithLongObject(l);
        signalWithLongObject(l);
    }
    public void triggerCallWithFloat(float f) {
        backendCallback.callWithFloat(f);
        signalWithFloat(f);
    }
    public void triggerCallWithFloatObject(Float f) {
        backendCallback.callWithFloatObject(f);
        signalWithFloatObject(f);
    }
    public void triggerCallWithChar(char c) {
        backendCallback.callWithChar(c);
        signalWithChar(c);
    }
    public void triggerCallWithCharObject(Character c) {
        backendCallback.callWithCharObject(c);
        signalWithCharObject(c);
    }
    public void triggerCallWithTwoInts(int a, int b) {
        backendCallback.callWithTwoInts(a, b);
        signalWithTwoInts(a, b);
    }
    public void triggerCallWithThreeInts(int a, int b, int c) {
        backendCallback.callWithThreeInts(a, b, c);
        signalWithThreeInts(a, b, c);
    }
    public void triggerCallWithIntAndString(int num, String str) {
        backendCallback.callWithIntAndString(num, str);
        signalWithIntAndString(num, str);
    }
    public void triggerCallWithBooleanAndDouble(boolean flag, double value) {
        backendCallback.callWithBooleanAndDouble(flag, value);
        signalWithBooleanAndDouble(flag, value);
    }
    public void triggerCallWithString(String str) {
        backendCallback.callWithString(str);
        signalWithString(str);
    }
    public void triggerCallWithList(java.util.List<String> list) {
        backendCallback.callWithList(list);
        signalWithList(list);
    }
    public void triggerCallWithMap(java.util.Map<String, Object> map) {
        backendCallback.callWithMap(map);
        signalWithMap(map);
    }
    public void triggerCallWithUrl(java.net.URI url) {
        backendCallback.callWithUrl(url);
        signalWithUrl(url);
    }
    public void triggerCallWithEnum() {
        backendCallback.callWithEnum(SingletonTestBackend.Color.BLUE);
        signalWithEnum(SingletonTestBackend.Color.BLUE);
    }

    public String callbackProxyToString() {
        return backendCallback.toString();
    }
}
