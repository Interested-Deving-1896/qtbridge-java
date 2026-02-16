/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.qtproject.qt.bridge.annotations.QMLSignals;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.core.QtProperty;

@QMLRegistrable(singleton = false)
public class ValueTypesTestBackend {
    public interface QmlCallback {
        void boxedIntSignal(Integer value);
        void unboxedIntSignal(int value);
        void boxedLongSignal(Long value);
        void unboxedLongSignal(long value);
        void boxedDoubleSignal(Double value);
        void unboxedDoubleSignal(double value);
        void boxedCharSignal(Character value);
        void unboxedCharSignal(char value);
        void boxedFloatSignal(Float value);
        void unboxedFloatSignal(float value);
        void boxedBoolSignal(Boolean value);
        void unboxedBoolSignal(boolean value);
        void boxedShortSignal(Short value);
        void unboxedShortSignal(short value);
        void boxedByteSignal(Byte value);
        void unboxedByteSignal(byte value);
        void stringSignal(String value);
        void uriSignal(URI value);
        void registrableSignal(ValueTypesMyType value);
        void mapSignal(Map<String, Object> value);
        void listSignal(List<Object> value);
        void stringListSignal(List<String> value);
        void enumSignal(Color value);
    }
    @QMLSignals
    QmlCallback qmlCallback;

    // Plain/raw arrays
    public QtProperty<int[]> intArrayProp = new QtProperty<>(new int[] {1, 2, 3});
    public QtProperty<String[]> stringArrayProp = new QtProperty<>(new String[] {"aa", "bb", "cc"});

    public int[] intArrayEcho(int[] value) {
        System.out.println("==== Java intArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public Integer[] IntegerArrayEcho(Integer[] value) {
        System.out.println("==== Java IntegerArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public String[] StringArrayEcho(String[] value) {
        System.out.println("==== Java StringArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public double[] doubleArrayEcho(double[] value) {
        System.out.println("==== Java doubleArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public Double[] DoubleArrayEcho(Double[] value) {
        System.out.println("==== Java DoubleArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public float[] floatArrayEcho(float[] value) {
        System.out.println("==== Java floatArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public Float[] FloatArrayEcho(Float[] value) {
        System.out.println("==== Java FloatArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public long[] longArrayEcho(long[] value) {
        System.out.println("==== Java longArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public Long[] LongArrayEcho(Long[] value) {
        System.out.println("==== Java LongArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public short[] shortArrayEcho(short[] value) {
        System.out.println("==== Java shortArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public Short[] ShortArrayEcho(Short[] value) {
        System.out.println("==== Java ShortArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public char[] charArrayEcho(char[] value) {
        System.out.println("==== Java charArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public Character[] CharacterArrayEcho(Character[] value) {
        System.out.println("==== Java CharacterArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public byte[] byteArrayEcho(byte[] value) {
        System.out.println("==== Java byteArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public Byte[] ByteArrayEcho(Byte[] value) {
        System.out.println("==== Java ByteArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public boolean[] booleanArrayEcho(boolean[] value) {
        System.out.println("==== Java booleanArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    public Boolean[] BooleanArrayEcho(Boolean[] value) {
        System.out.println("==== Java BooleanArray echo got array: " + Arrays.toString(value));
        // todo emit signal
        return value;
    }

    // Map
    public QtProperty<Map<String,Object>> mapProp =
            new QtProperty<>(Map.of("k1", 1, "k2", 2));
    public Map<String,Object> mapEcho(Map<String,Object> value) {
        qmlCallback.mapSignal(value);
        return value;
    }
    public void mapPropertyNull() {
        mapProp = null;
    }
    public void mapValueNull() {
        mapProp.setValue(null);
    }
    public void setMapProperty(Map<String,Object> value) {
        mapProp.setValue(value);
    }

    // List
    public QtProperty<List<Object> > listProp = new QtProperty<>(List.of("aa", "bb"));
    public List<Object> listEcho(List<Object> value) {
        qmlCallback.listSignal(value);
        return value;
    }
    public void listPropertyNull() {
        listProp = null;
    }
    public void listValueNull() {
        listProp.setValue(null);
    }
    public void setListProperty(List<Object> value) {
        listProp.setValue(value);
    }

    // List<String>
    public QtProperty<List<String> > stringListProp = new QtProperty<>(List.of("aa", "bb"));
    public List<String> stringListEcho(List<String> value) {
        qmlCallback.stringListSignal(value);
        return value;
    }
    public void stringListPropertyNull() {
        stringListProp = null;
    }
    public void stringListValueNull() {
        stringListProp.setValue(null);
    }
    public void setStringListProperty(List<String> value) {
        stringListProp.setValue(value);
    }

    // Enum
    public QtProperty<Color> enumProp = new QtProperty<>(Color.RED);
    public void enumChange() {
        enumProp.setValue(enumProp.getValue() == Color.RED ? Color.BLUE : Color.RED);
    }
    public void enumPropertyNull() {
        enumProp = null;
    }
    public void enumValueNull() {
        enumProp.setValue(null);
    }
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
    public void setEnumProperty(String value) {
        if (value.equals("RED"))
            enumProp.setValue(Color.RED);
        else if (value.equals("BLUE"))
            enumProp.setValue(Color.BLUE);
        else if (value.equals("GREEN"))
            enumProp.setValue(Color.GREEN);
    }

    // Registrable
    public QtProperty<ValueTypesMyType> registrableProp =
            new QtProperty<>(new ValueTypesMyType(11));
    public ValueTypesMyType registrableEcho(ValueTypesMyType value) {
        qmlCallback.registrableSignal(value);
        return value;
    }
    public void registrablePropertyNull() {
        registrableProp = null;
    }
    public void registrableValueNull() {
        registrableProp.setValue(null);
    }
    public void setRegistrableProperty(ValueTypesMyType value) {
        registrableProp.setValue(value);
    }

    // URI
    public QtProperty<URI> uriProp = new QtProperty<>(URI.create("http://x.a/a"));
    public URI uriEcho(URI value) {
        qmlCallback.uriSignal(value);
        return value;
    };
    public void uriPropertyNull() {
        uriProp = null;
    }
    public void uriValueNull() {
        uriProp.setValue(null);
    }
    public void setUriProperty(URI value) {
        uriProp.setValue(value);
    }

    // Long, long
    public QtProperty<Long> longProp = new QtProperty<>(2L);
    public Long boxedLongEcho(Long value) {
        qmlCallback.boxedLongSignal(value);
        return value;
    };
    public long unboxedLongEcho(long value) {
        qmlCallback.unboxedLongSignal(value);
        return value;
    };
    public void longPropertyNull() {
        longProp = null;
    }
    public void longValueNull() {
        longProp.setValue(null);
    }
    public void setLongProperty(long value) {
        longProp.setValue(value);
    }

    // String
    public QtProperty<String> stringProp = new QtProperty<>("aa");
    public String stringEcho(String value) {
        qmlCallback.stringSignal(value);
        return value;
    };
    public void stringPropertyNull() {
        stringProp = null;
    }
    public void stringValueNull() {
        stringProp.setValue(null);
    }
    public void setStringProperty(String value) {
        stringProp.setValue(value);
    }

    // Integer, int
    public QtProperty<Integer> intProp = new QtProperty<>(2);
    public Integer boxedIntEcho(Integer value) {
        qmlCallback.boxedIntSignal(value);
        return value;
    };
    public int unboxedIntEcho(int value) {
        qmlCallback.unboxedIntSignal(value);
        return value;
    };
    public void intPropertyNull() {
        intProp = null;
    }
    public void intValueNull() {
        intProp.setValue(null);
    }
    public void setIntProperty(int value) {
        intProp.setValue(value);
    }

    // Double, double
    public QtProperty<Double> doubleProp = new QtProperty<>(2.2d);
    public Double boxedDoubleEcho(Double value) {
        qmlCallback.boxedDoubleSignal(value);
        return value;
    };
    public double unboxedDoubleEcho(double value) {
        qmlCallback.unboxedDoubleSignal(value);
        return value;
    };
    public void doublePropertyNull() {
        doubleProp = null;
    }
    public void doubleValueNull() {
        doubleProp.setValue(null);
    }
    public void setDoubleProperty(double value) {
        doubleProp.setValue(value);
    }

    // Float, float
    public QtProperty<Float> floatProp = new QtProperty<>(2.2f);
    public Float boxedFloatEcho(Float value) {
        qmlCallback.boxedFloatSignal(value);
        return value;
    };
    public float unboxedFloatEcho(float value) {
        qmlCallback.unboxedFloatSignal(value);
        return value;
    };
    public void floatPropertyNull() {
        floatProp = null;
    }
    public void floatValueNull() {
        floatProp.setValue(null);
    }
    public void setFloatProperty(float value) {
        floatProp.setValue(value);
    }

    // Boolean, boolean
    public QtProperty<Boolean> boolProp = new QtProperty<>(true);
    public Boolean boxedBoolEcho(Boolean value) {
        qmlCallback.boxedBoolSignal(value);
        return value;
    };
    public boolean unboxedBoolEcho(boolean value) {
        qmlCallback.unboxedBoolSignal(value);
        return value;
    };
    public void boolPropertyNull() {
        boolProp = null;
    }
    public void boolValueNull() {
        boolProp.setValue(null);
    }
    public void setBoolProperty(boolean value) {
        boolProp.setValue(value);
    }

    // Character, char
    public QtProperty<Character> charProp = new QtProperty<>('a');
    public Character boxedCharEcho(Character value) {
        qmlCallback.boxedCharSignal(value);
        return value;
    };
    public char unboxedCharEcho(char value) {
        qmlCallback.unboxedCharSignal(value);
        return value;
    };
    public void charPropertyNull() {
        charProp = null;
    }
    public void charValueNull() {
        charProp.setValue(null);
    }
    public void setCharProperty(char value) {
        charProp.setValue(value);
    }

    // Byte, byte
    public QtProperty<Byte> byteProp = new QtProperty<>((byte)52);
    public Byte boxedByteEcho(Byte value) {
        qmlCallback.boxedByteSignal(value);
        return value;
    };
    public byte unboxedByteEcho(byte value) {
        qmlCallback.unboxedByteSignal(value);
        return value;
    };
    public void bytePropertyNull() {
        byteProp = null;
    }
    public void byteValueNull() {
        byteProp.setValue(null);
    }
    public void setByteProperty(byte value) {
        byteProp.setValue(value);
    }

    // Short, short
    public QtProperty<Short> shortProp = new QtProperty<>((short)52);
    public Short boxedShortEcho(Short value) {
        qmlCallback.boxedShortSignal(value);
        return value;
    };
    public short unboxedShortEcho(short value) {
        qmlCallback.unboxedShortSignal(value);
        return value;
    };
    public void shortPropertyNull() {
        shortProp = null;
    }
    public void shortValueNull() {
        shortProp.setValue(null);
    }
    public void setShortProperty(short value) {
        shortProp.setValue(value);
    }
}
