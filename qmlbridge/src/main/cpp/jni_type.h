/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#ifndef JNI_TYPE_H
#define JNI_TYPE_H

#define DEFINE_JAVA_TYPE_2(tagName, classPath) DEFINE_JAVA_TYPE_3(tagName, classPath, nullptr)

#define DEFINE_JAVA_TYPE_3(tagName, classPath, ctorSig)                                              \
    struct tagName                                                                                   \
    {                                                                                                \
        static constexpr const char *className() { return classPath; }                               \
        static constexpr const char *constructorSignature() { return ctorSig; }                      \
        static constexpr char typeSignatureLiteral[] = "L" classPath ";";                            \
        static constexpr const char* typeSignature() { return typeSignatureLiteral; }                \
    }

#define GET_MACRO(_1, _2, _3, NAME, ...) NAME
#define EXPAND(x) x
#define DEFINE_JAVA_TYPE(...)                                                                        \
    EXPAND(GET_MACRO(__VA_ARGS__, DEFINE_JAVA_TYPE_3, DEFINE_JAVA_TYPE_2)(__VA_ARGS__))

DEFINE_JAVA_TYPE(JavaLangString, "java/lang/String","(Ljava/lang/String;)V");
DEFINE_JAVA_TYPE(JavaLangInteger, "java/lang/Integer", "(I)V");
DEFINE_JAVA_TYPE(JavaLangBoolean, "java/lang/Boolean", "(Z)V");
DEFINE_JAVA_TYPE(JavaLangDouble, "java/lang/Double", "(D)V");
DEFINE_JAVA_TYPE(JavaLangFloat, "java/lang/Float", "(F)V");
DEFINE_JAVA_TYPE(JavaLangLong, "java/lang/Long", "(J)V");
DEFINE_JAVA_TYPE(JavaLangByte, "java/lang/Byte", "(B)V");
DEFINE_JAVA_TYPE(JavaLangShort, "java/lang/Short", "(S)V");
DEFINE_JAVA_TYPE(JavaLangCharacter, "java/lang/Character", "(C)V");

DEFINE_JAVA_TYPE(JavaList, "java/util/List");
DEFINE_JAVA_TYPE(JavaMap, "java/util/Map");
DEFINE_JAVA_TYPE(JavaSet, "java/util/Set");
DEFINE_JAVA_TYPE(JavaIterator, "java/util/Iterator");
DEFINE_JAVA_TYPE(JavaMapEntry, "java/util/Map$Entry");
DEFINE_JAVA_TYPE(JavaArrayList, "java/util/ArrayList", "()V");
DEFINE_JAVA_TYPE(JavaHashMap, "java/util/HashMap", "()V");
DEFINE_JAVA_TYPE(JavaHashSet, "java/util/HashSet", "()V");

DEFINE_JAVA_TYPE(JavaNetURI, "java/net/URI", "(Ljava/lang/String;)V");

DEFINE_JAVA_TYPE(JavaLangObject, "java/lang/Object");
DEFINE_JAVA_TYPE(JavaLangClass, "java/lang/Class");
DEFINE_JAVA_TYPE(JavaLangEnum, "java/lang/Enum");
DEFINE_JAVA_TYPE(JavaLangReflectField, "java/lang/reflect/Field");

DEFINE_JAVA_TYPE(JavaQtObject, "org/qtproject/qt/bridge/core/QtObject");
DEFINE_JAVA_TYPE(JavaQtProperty, "org/qtproject/qt/bridge/core/QtProperty");
DEFINE_JAVA_TYPE(JavaQtListModel, "org/qtproject/qt/bridge/core/QtListModel");
DEFINE_JAVA_TYPE(JavaQtQmlRegistration, "org/qtproject/qt/bridge/core/QtQmlRegistration");
DEFINE_JAVA_TYPE(JavaQtQmlChildren, "org/qtproject/qt/bridge/core/QtQmlChildren");

DEFINE_JAVA_TYPE(JavaIllegalStateException, "java/lang/IllegalStateException");

#endif // JNI_TYPE_H
