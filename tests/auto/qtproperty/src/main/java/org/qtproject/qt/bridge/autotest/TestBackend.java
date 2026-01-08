/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.qtproject.qt.bridge.annotations.QMLSignals;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.core.QtProperty;
import org.qtproject.qt.bridge.core.QtPropertyObserver;

@QMLRegistrable(name = "TestBackend", singleton = true)
public class TestBackend {
    private final List<String> stringListValues =
            new ArrayList<>(Arrays.asList("Zero", "One", "Two", "Three", "Four"));
    private final Map<String, Object> mapValues =
            Map.of("key1", "value1", "key2", "value2");

    public interface QmlCallback {
        void integerPropertyValueChanged();
        void stringListPropertyValueChanged();
        void mapPropertyValueChanged();
        void listPropertyValueChanged();
        void urlPropertyValueChanged();
        void enumPropertyValueChanged();
        void nonAs€iiäöPropertyValueChanged();
    }
    @QMLSignals
    QmlCallback qmlCallback;

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

    public QtProperty<Color> enumProperty = new QtProperty<>(Color.RED);
    public Color enumPropertyValue() {
        return enumProperty.getValue();
    }
    public void changeEnumProperty() { enumProperty.setValue(Color.GREEN); }

    public QtProperty<URI> urlProperty = new QtProperty<>(URI.create("http://example.com/path"));
    public URI urlPropertyValue() { return urlProperty.getValue(); }
    public void changeUrlPropertyValue() { urlProperty.setValue(URI.create("http://changed.example.com/path")); }

    public QtProperty<Integer> integerProperty = new QtProperty<>(42);
    public Integer integerPropertyValue() { return integerProperty.getValue(); }
    public void incrementIntegerProperty() {
        integerProperty.setValue(integerProperty.getValue() + 1);
    }

    public QtProperty<List<String>> stringListProperty = new QtProperty<>(stringListValues);
    public void changeStringListProperty() {
        stringListProperty.setValue(new ArrayList<>(Arrays.asList("AAA", "BBB", "CCC")));
    }
    public List<String> stringListPropertyValue() {
        return stringListProperty.getValue();
    }

    public QtProperty<List<Object>> listProperty = new QtProperty<>(null);

    public QtProperty<Integer> nonAs€iiäöProperty = new QtProperty<>(123);

    public QtProperty<Map<String, Object>> mapProperty = new QtProperty<>(mapValues);
    public void changeMapProperty() {
        mapProperty.setValue(Map.of("key3", "value3", "key4", "value4"));
    }
    public Map<String, Object> mapPropertyValue() { return mapProperty.getValue(); }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE  = new TypeReference<>() {};
    private static final TypeReference<List<Object>>        LIST_TYPE = new TypeReference<>() {};

    public void setJsonObject(String json) {
        Map<String, Object> map = null;
        try {
            map = MAPPER.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            // System.out.println(e);
        }
        mapProperty.setValue(map);
    }

    public void setJsonArray(String json) {
        List<Object> list = null;
        try {
            list = MAPPER.readValue(json, LIST_TYPE);
        } catch (Exception e) {
            // System.out.println(e);
        }
        listProperty.setValue(list);
    }

    public void registerManualObservers() {
        // Create with the "valueChanged" factory method
        QtPropertyObserver obs1 = QtPropertyObserver.valueChanged(()
                -> { qmlCallback.integerPropertyValueChanged(); });
        QtPropertyObserver obs2 = new QtPropertyObserver() {
            @Override public void onValueChanged() {
                qmlCallback.integerPropertyValueChanged();
            }
        };
        manualObservers.add(obs1);
        manualObservers.add(obs2);
        integerProperty.observe(obs1);
        integerProperty.observe(obs2);
    }
    private ArrayList<QtPropertyObserver> manualObservers = new ArrayList<>();
    private ArrayList<AutoCloseable> observers = new ArrayList<>();
    public void startObserving() {
        if (!observers.isEmpty())
            return;
        observers.add(integerProperty.onValueChanged(()
                -> { qmlCallback.integerPropertyValueChanged(); }));
        observers.add(stringListProperty.onValueChanged(()
                -> { qmlCallback.stringListPropertyValueChanged(); }));
        observers.add(mapProperty.onValueChanged(()
                -> { qmlCallback.mapPropertyValueChanged(); }));
        observers.add(listProperty.onValueChanged(()
                -> { qmlCallback.listPropertyValueChanged(); }));
        observers.add(urlProperty.onValueChanged(()
                -> { qmlCallback.urlPropertyValueChanged(); }));
        observers.add(enumProperty.onValueChanged(()
                -> { qmlCallback.enumPropertyValueChanged(); }));
        observers.add(nonAs€iiäöProperty.onValueChanged(()
                -> { qmlCallback.nonAs€iiäöPropertyValueChanged(); }));
    }
    public void stopObserving() {
        for (AutoCloseable observer : observers) {
            try { observer.close(); } catch(Exception ignored) {}
        }
        for (QtPropertyObserver observer : manualObservers) {
            integerProperty.removeObserver(observer);
        }
        observers.clear();
    }

    // Nested type
    public static class NestedClass {
        public String nestedString = "€€nested";
        public int nestedInt = 123;
    }

    public static class ParentClass {
        public int parentInt = 12;
        public String parentString = "€€parent";
        protected String parentProtectedString = "parentProtected";
    }

    public static class ChildClass extends ParentClass {
        public int childint = 7;
        public Integer childInteger = 8;
        public float childfloat = 3.14f;
        public Float childFloat = 4.14f;
        public double childdouble = 5.14d;
        public Double childDouble = 6.14d;
        public short childshort = 2;
        public Short childShort = 3;
        public boolean childboolean = true;
        public Boolean childBoolean = Boolean.FALSE;
        public long childlong = 9L;
        public Long childLong = 10L;
        public byte childbyte = 11;
        public Byte childByte = (byte) 12;
        public char childchar = 'a';
        public Character childCharacter = 'b';

        public String childString = "€€child";
        public java.net.URI childURI = java.net.URI.create("https://example.com");

        public java.util.List<Object> childList =
                java.util.List.of("tag", 1, true, new NestedClass());
        public java.util.Map<String, Object> childMap =
                java.util.Map.of("k1", "v1", "k2", 2, "k3", new NestedClass());

        public Color childEnum = Color.RED;
        // Nested object to test recursive mapping
        public NestedClass childNestedClass = new NestedClass();
        // Private field to test includeNonPublic=true
        private String childPrivateString = "shh";
    }

    public void setObjectMapPublicOnly() {
        mapProperty.setValue(QtProperty.toMap(new ChildClass()));
    }
    public void setObjectMapIncludeNonPublic() {
        mapProperty.setValue(QtProperty.toMap(new ChildClass(), true));
    }
    public void setNullObjectMap() {
        mapProperty.setValue(null);
    }
}
