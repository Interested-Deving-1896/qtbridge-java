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
        void intArraySignal(int[] value);
        void IntegerArraySignal(Integer[] value);
        void StringArraySignal(String[] value);
        void doubleArraySignal(double[] value);
        void DoubleArraySignal(Double[] value);
        void floatArraySignal(float[] value);
        void FloatArraySignal(Float[] value);
        void longArraySignal(long[] value);
        void LongArraySignal(Long[] value);
        void shortArraySignal(short[] value);
        void ShortArraySignal(Short[] value);
        void charArraySignal(char[] value);
        void CharacterArraySignal(Character[] value);
        void byteArraySignal(byte[] value);
        void ByteArraySignal(Byte[] value);
        void booleanArraySignal(boolean[] value);
        void BooleanArraySignal(Boolean[] value);
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
    public QtProperty<Integer[]> integerArrayProp = new QtProperty<>(new Integer[] {1, 2, 3});
    public QtProperty<double[]> doubleArrayProp = new QtProperty<>(new double[] {1.1, 2.2, 3.3});
    public QtProperty<Double[]> boxedDoubleArrayProp = new QtProperty<>(new Double[] {1.1, 2.2, 3.3});
    public QtProperty<float[]> floatArrayProp = new QtProperty<>(new float[] {1.5f, 2.5f, 3.5f});
    public QtProperty<Float[]> boxedFloatArrayProp = new QtProperty<>(new Float[] {1.5f, 2.5f, 3.5f});
    public QtProperty<long[]> longArrayProp = new QtProperty<>(new long[] {1001, 1002, 1003});
    public QtProperty<Long[]> boxedLongArrayProp = new QtProperty<>(new Long[] {1001L, 1002L, 1003L});
    public QtProperty<short[]> shortArrayProp = new QtProperty<>(new short[] {11, 22, 33});
    public QtProperty<Short[]> boxedShortArrayProp = new QtProperty<>(new Short[] {11, 22, 33});
    public QtProperty<char[]> charArrayProp = new QtProperty<>(new char[] {'a', 'b', 'c'});
    public QtProperty<Character[]> boxedCharArrayProp = new QtProperty<>(new Character[] {'a', 'b', 'c'});
    public QtProperty<byte[]> byteArrayProp = new QtProperty<>(new byte[] {1, 2, 3});
    public QtProperty<Byte[]> boxedByteArrayProp = new QtProperty<>(new Byte[] {1, 2, 3});
    public QtProperty<boolean[]> booleanArrayProp = new QtProperty<>(new boolean[] {true, false, true});
    public QtProperty<Boolean[]> boxedBooleanArrayProp =
            new QtProperty<>(new Boolean[] {true, false, true});
    public QtProperty<String[]> stringArrayProp = new QtProperty<>(new String[] {"aa", "bb", "cc"});

    public int[] intArrayEcho(int[] value) {
        qmlCallback.intArraySignal(value);
        return value;
    }

    public Integer[] IntegerArrayEcho(Integer[] value) {
        qmlCallback.IntegerArraySignal(value);
        return value;
    }

    public String[] StringArrayEcho(String[] value) {
        qmlCallback.StringArraySignal(value);
        return value;
    }

    public double[] doubleArrayEcho(double[] value) {
        qmlCallback.doubleArraySignal(value);
        return value;
    }

    public Double[] DoubleArrayEcho(Double[] value) {
        qmlCallback.DoubleArraySignal(value);
        return value;
    }

    public float[] floatArrayEcho(float[] value) {
        qmlCallback.floatArraySignal(value);
        return value;
    }

    public Float[] FloatArrayEcho(Float[] value) {
        qmlCallback.FloatArraySignal(value);
        return value;
    }

    public long[] longArrayEcho(long[] value) {
        qmlCallback.longArraySignal(value);
        return value;
    }

    public Long[] LongArrayEcho(Long[] value) {
        qmlCallback.LongArraySignal(value);
        return value;
    }

    public short[] shortArrayEcho(short[] value) {
        qmlCallback.shortArraySignal(value);
        return value;
    }

    public Short[] ShortArrayEcho(Short[] value) {
        qmlCallback.ShortArraySignal(value);
        return value;
    }

    public char[] charArrayEcho(char[] value) {
        qmlCallback.charArraySignal(value);
        return value;
    }

    public Character[] CharacterArrayEcho(Character[] value) {
        qmlCallback.CharacterArraySignal(value);
        return value;
    }

    public byte[] byteArrayEcho(byte[] value) {
        qmlCallback.byteArraySignal(value);
        return value;
    }

    public Byte[] ByteArrayEcho(Byte[] value) {
        qmlCallback.ByteArraySignal(value);
        return value;
    }

    public boolean[] booleanArrayEcho(boolean[] value) {
        qmlCallback.booleanArraySignal(value);
        return value;
    }

    public Boolean[] BooleanArrayEcho(Boolean[] value) {
        qmlCallback.BooleanArraySignal(value);
        return value;
    }

    public void intArrayValueNull() {
        intArrayProp.setValue(null);
    }

    public void integerArrayValueNull() {
        integerArrayProp.setValue(null);
    }

    public void doubleArrayValueNull() {
        doubleArrayProp.setValue(null);
    }

    public void boxedDoubleArrayValueNull() {
        boxedDoubleArrayProp.setValue(null);
    }

    public void floatArrayValueNull() {
        floatArrayProp.setValue(null);
    }

    public void boxedFloatArrayValueNull() {
        boxedFloatArrayProp.setValue(null);
    }

    public void longArrayValueNull() {
        longArrayProp.setValue(null);
    }

    public void boxedLongArrayValueNull() {
        boxedLongArrayProp.setValue(null);
    }

    public void shortArrayValueNull() {
        shortArrayProp.setValue(null);
    }

    public void boxedShortArrayValueNull() {
        boxedShortArrayProp.setValue(null);
    }

    public void charArrayValueNull() {
        charArrayProp.setValue(null);
    }

    public void boxedCharArrayValueNull() {
        boxedCharArrayProp.setValue(null);
    }

    public void byteArrayValueNull() {
        byteArrayProp.setValue(null);
    }

    public void boxedByteArrayValueNull() {
        boxedByteArrayProp.setValue(null);
    }

    public void booleanArrayValueNull() {
        booleanArrayProp.setValue(null);
    }

    public void boxedBooleanArrayValueNull() {
        boxedBooleanArrayProp.setValue(null);
    }

    public void stringArrayValueNull() {
        stringArrayProp.setValue(null);
    }

    public void intArrayPropertyNull() {
        intArrayProp = null;
    }

    public void integerArrayPropertyNull() {
        integerArrayProp = null;
    }

    public void doubleArrayPropertyNull() {
        doubleArrayProp = null;
    }

    public void boxedDoubleArrayPropertyNull() {
        boxedDoubleArrayProp = null;
    }

    public void floatArrayPropertyNull() {
        floatArrayProp = null;
    }

    public void boxedFloatArrayPropertyNull() {
        boxedFloatArrayProp = null;
    }

    public void longArrayPropertyNull() {
        longArrayProp = null;
    }

    public void boxedLongArrayPropertyNull() {
        boxedLongArrayProp = null;
    }

    public void shortArrayPropertyNull() {
        shortArrayProp = null;
    }

    public void boxedShortArrayPropertyNull() {
        boxedShortArrayProp = null;
    }

    public void charArrayPropertyNull() {
        charArrayProp = null;
    }

    public void boxedCharArrayPropertyNull() {
        boxedCharArrayProp = null;
    }

    public void byteArrayPropertyNull() {
        byteArrayProp = null;
    }

    public void boxedByteArrayPropertyNull() {
        boxedByteArrayProp = null;
    }

    public void booleanArrayPropertyNull() {
        booleanArrayProp = null;
    }

    public void boxedBooleanArrayPropertyNull() {
        boxedBooleanArrayProp = null;
    }

    public void stringArrayPropertyNull() {
        stringArrayProp = null;
    }

    public void setIntArrayProperty(int[] value) {
        intArrayProp.setValue(value);
    }

    public void setIntegerArrayProperty(Integer[] value) {
        integerArrayProp.setValue(value);
    }

    public void setDoubleArrayProperty(double[] value) {
        doubleArrayProp.setValue(value);
    }

    public void setBoxedDoubleArrayProperty(Double[] value) {
        boxedDoubleArrayProp.setValue(value);
    }

    public void setFloatArrayProperty(float[] value) {
        floatArrayProp.setValue(value);
    }

    public void setBoxedFloatArrayProperty(Float[] value) {
        boxedFloatArrayProp.setValue(value);
    }

    public void setLongArrayProperty(long[] value) {
        longArrayProp.setValue(value);
    }

    public void setBoxedLongArrayProperty(Long[] value) {
        boxedLongArrayProp.setValue(value);
    }

    public void setShortArrayProperty(short[] value) {
        shortArrayProp.setValue(value);
    }

    public void setBoxedShortArrayProperty(Short[] value) {
        boxedShortArrayProp.setValue(value);
    }

    public void setCharArrayProperty(char[] value) {
        charArrayProp.setValue(value);
    }

    public void setBoxedCharArrayProperty(Character[] value) {
        boxedCharArrayProp.setValue(value);
    }

    public void setByteArrayProperty(byte[] value) {
        byteArrayProp.setValue(value);
    }

    public void setBoxedByteArrayProperty(Byte[] value) {
        boxedByteArrayProp.setValue(value);
    }

    public void setBooleanArrayProperty(boolean[] value) {
        booleanArrayProp.setValue(value);
    }

    public void setBoxedBooleanArrayProperty(Boolean[] value) {
        boxedBooleanArrayProp.setValue(value);
    }

    public void setStringArrayProperty(String[] value) {
        stringArrayProp.setValue(value);
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
