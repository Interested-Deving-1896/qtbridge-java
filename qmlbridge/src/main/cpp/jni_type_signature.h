// Copyright (C) 2025 The Qt Company Ltd.
// SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only

#ifndef JNI_SIGNATURE_H
#define JNI_SIGNATURE_H

#include <jni.h>

#include <cstddef>
#include <type_traits>

template<std::size_t N>
struct ConstexprString {
    char value[N]{};
    constexpr ConstexprString() = default;
    constexpr ConstexprString(const char (&str)[N])
    {
        for (std::size_t i = 0; i < N-1; ++i)
            value[i] = str[i];
    }
    constexpr operator const char *() const { return value; }
    constexpr std::size_t size() const { return N-1; }
};

template<std::size_t N>
constexpr ConstexprString<N> makeConstexpr(const char (&str)[N])
{
    return ConstexprString<N>{str};
}
template<typename T>
constexpr auto concat(T t)
{
    return t;
}

constexpr auto concat()
{
    return makeConstexpr("");
}

template<typename First, typename Second, typename... Rest>
constexpr auto concat(First first, Second second, Rest... rest)
{
    return concat(concat(first, second), rest...);
}

template<std::size_t N1, std::size_t N2>
constexpr auto concat(ConstexprString<N1> a, ConstexprString<N2> b)
{
    ConstexprString<N1 + N2 - 1> result{};
    for (std::size_t i = 0; i < N1 - 1; ++i)
        result.value[i] = a.value[i];
    for (std::size_t i = 0; i < N2 - 1; ++i)
        result.value[i + N1 - 1] = b.value[i];
    return result;
}

template<std::size_t N1, std::size_t N2>
constexpr auto operator+(ConstexprString<N1> a, ConstexprString<N2> b)
{
    return concat(a, b);
}

template<typename T, typename = void>
struct JNISignature {
    static_assert(sizeof(T) == -1, "Unsupported JNI type");
};

template<typename JavaType>
struct JNISignature<JavaType, std::void_t<decltype(JavaType::typeSignatureLiteral)>> {
    static constexpr auto value() { return makeConstexpr(JavaType::typeSignatureLiteral); }
};

// Primitive JNI types
template<>
struct JNISignature<void> {
    static constexpr auto value() { return makeConstexpr("V"); }
};
template<>
struct JNISignature<jboolean> {
    static constexpr auto value() { return makeConstexpr("Z"); }
};
template<>
struct JNISignature<bool> {
    static constexpr auto value() { return makeConstexpr("Z"); }
};
template<>
struct JNISignature<jbyte> {
    static constexpr auto value() { return makeConstexpr("B"); }
};
template<>
struct JNISignature<jchar> {
    static constexpr auto value() { return makeConstexpr("C"); }
};
template<>
struct JNISignature<jshort> {
    static constexpr auto value() { return makeConstexpr("S"); }
};
template<>
struct JNISignature<jint> {
    static constexpr auto value() { return makeConstexpr("I"); }
};
template<>
struct JNISignature<jlong> {
    static constexpr auto value() { return makeConstexpr("J"); }
};
template<>
struct JNISignature<jfloat> {
    static constexpr auto value() { return makeConstexpr("F"); }
};
template<>
struct JNISignature<jdouble> {
    static constexpr auto value() { return makeConstexpr("D"); }
};
template<>
struct JNISignature<jstring> {
    static constexpr auto value() { return makeConstexpr("Ljava/lang/String;"); }
};
template<>
struct JNISignature<jobject> {
    static constexpr auto value() { return makeConstexpr("Ljava/lang/Object;"); }
};
template<>
struct JNISignature<jclass> {
    static constexpr auto value() { return makeConstexpr("Ljava/lang/Class;"); }
};

// qt-convenience
template<>
struct JNISignature<QString> {
    static constexpr auto value() { return makeConstexpr("Ljava/lang/String;"); }
};
template<>
struct JNISignature<QVariant> {
    static constexpr auto value() { return makeConstexpr("Ljava/lang/Object;"); }
};
template<typename>
struct JNIArray {};

template<typename Element>
struct JNISignature<JNIArray<Element>> {
    static constexpr auto value()
    {
        return concat(makeConstexpr("["), JNISignature<Element>::value());
    }
};

#endif
