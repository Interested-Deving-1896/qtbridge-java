/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#ifndef JNI_METHOD_INVOKER_H
#define JNI_METHOD_INVOKER_H

#include "converter.h"

#include <QtCore/qmetaobject.h>

#include <jni.h>

namespace Utility::JNI
{
    static bool checkAndClearException(JNIEnv* env)
    {
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            return true;
        }
        return false;
    }

    template <typename JavaType = void, typename ValueType>
    static jvalue wrapJValue(JNIEnv* env, ValueType value)
    {
        jvalue jvalueResult{};
        if constexpr (std::is_same_v<JavaType, void>) {
            if constexpr (std::is_same_v<ValueType, jint> || std::is_same_v<ValueType, int>) {
                jvalueResult.i = value;
            } else if constexpr (std::is_same_v<ValueType, jboolean> || std::is_same_v<ValueType, bool>) {
                jvalueResult.z = value;
            } else if constexpr (std::is_same_v<ValueType, jfloat> || std::is_same_v<ValueType, float>) {
                jvalueResult.f = value;
            } else if constexpr (std::is_same_v<ValueType, jdouble> || std::is_same_v<ValueType, double>) {
                jvalueResult.d = value;
            } else if constexpr (std::is_same_v<ValueType, jlong> || std::is_same_v<ValueType, qint64>) {
                jvalueResult.j = value;
            } else if constexpr (std::is_same_v<ValueType, jshort> || std::is_same_v<ValueType, short>) {
                jvalueResult.s = value;
            } else if constexpr (std::is_same_v<ValueType, jbyte>) {
                jvalueResult.b = value;
            } else if constexpr (std::is_same_v<ValueType, jchar> || std::is_same_v<ValueType, char>) {
                jvalueResult.c = value;
            } else if constexpr (std::is_same_v<ValueType, QChar>) {
                jvalueResult.c = value.unicode();
            } else if constexpr (std::is_same_v<ValueType, QString>) {
                jvalueResult.l = env->NewString(reinterpret_cast<const jchar *>(value.utf16()), value.size());
                checkAndClearException(env);
            }  else if constexpr (std::is_same_v<ValueType, QVariant>) {
                jvalueResult.l = Converter::convertQVariantToObject(value);
                checkAndClearException(env);
            } else if constexpr (std::is_same_v<ValueType, QVariantList>) {
                jvalueResult.l = Converter::convertQVariantListToObject(value);
                checkAndClearException(env);
            } else if constexpr (std::is_same_v<ValueType, QVariantMap>) {
                jvalueResult.l = Converter::convertQVariantMapToObject(value);
                checkAndClearException(env);
            } else if constexpr (std::is_same_v<ValueType, QStringList>) {
                jvalueResult.l = Converter::convertQStringListToObject(value);
                checkAndClearException(env);
            } else {
                jvalueResult.l = value;
            }
        } else {
            static_assert(sizeof(ValueType) == 0, "Unsupported type for JNI conversion");
        }
        return jvalueResult;
    }

    class JNIMethodInvoker
    {
    public:
        template<typename ReturnType, typename... Args>
        static auto invokeMethod(JNIEnv *env, const jobject javaObject, const jmethodID methodId, Args... args)
        {
            if constexpr (sizeof...(args) == 0) {
                if constexpr (std::is_same_v<ReturnType, void>) {
                    env->CallVoidMethod(javaObject, methodId);
                    checkAndClearException(env);
                } else {
                    return invokeGetter<ReturnType>(env, javaObject, methodId);
                }
            } else {
                const jvalue jniArgs[] = {wrapJValue(env, args)...};
                return invokeMethodWithJValues<ReturnType>(env, javaObject, methodId, jniArgs);
            }
        }

        template<typename ReturnType, typename... Args>
        static auto invokeStaticMethod(JNIEnv *env, jclass javaClass, const jmethodID methodId, Args... args)
        {
            if constexpr (sizeof...(args) == 0) {
                if constexpr (std::is_same_v<ReturnType, void>) {
                    env->CallStaticVoidMethod(javaClass, methodId);
                    checkAndClearException(env);
                } else {
                    return invokeStaticGetter<ReturnType>(env, javaClass, methodId);
                }
            } else {
                const jvalue jniArgs[] = {wrapJValue(env, args)...};
                return invokeStaticMethodWithJValues<ReturnType>(env, javaClass, methodId, jniArgs);
            }
        }

        template<typename ReturnType>
        static auto invokeMethodWithJValues(JNIEnv *env, jobject javaObject, jmethodID methodId, const jvalue *jniArgs);

        template<typename ReturnType>
        static auto invokeStaticMethodWithJValues(JNIEnv *env, jclass javaClass, const jmethodID methodId, const jvalue *jniArgs)
        {
            if constexpr (std::is_same_v<ReturnType, void>) {
                env->CallStaticVoidMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
            } else if constexpr (std::is_same_v<ReturnType, jint>) {
                const auto result = env->CallStaticIntMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jboolean>) {
                const auto result = env->CallStaticBooleanMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, bool>) {
                const auto result = env->CallStaticBooleanMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return static_cast<bool>(result);
            } else if constexpr (std::is_same_v<ReturnType, jfloat>) {
                const auto result = env->CallStaticFloatMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jdouble>) {
                auto result = env->CallStaticDoubleMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jlong>) {
                const auto result = env->CallStaticLongMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, qint64>) {
                const auto result = env->CallStaticLongMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return static_cast<qint64>(result);
            } else if constexpr (std::is_same_v<ReturnType, jshort>) {
                const auto result = env->CallStaticShortMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jbyte>) {
                const auto result = env->CallStaticByteMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jchar>) {
                const auto result = env->CallStaticCharMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, QChar>) {
                const auto result = env->CallStaticCharMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return QChar(result);
            } else if constexpr (std::is_same_v<ReturnType, jobject>) {
                auto result = env->CallStaticObjectMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jstring>) {
                const auto result = static_cast<jstring>(env->CallStaticObjectMethodA(javaClass, methodId, jniArgs));
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, QString>) {
                const auto jstr = static_cast<jstring>(env->CallStaticObjectMethodA(javaClass, methodId, jniArgs));
                if (checkAndClearException(env)) {
                    return QString();
                }
                return toQString(jstr);
            } else if constexpr (std::is_same_v<ReturnType, QVariant>) {
                const auto javaValue = env->CallStaticObjectMethodA(javaClass, methodId, jniArgs);
                if (checkAndClearException(env)) {
                    return QVariant();
                }
                return Converter::convertObjectToQVariant(javaValue);
            } else if constexpr (std::is_same_v<ReturnType, QVariantList>) {
                const auto javaValue = env->CallStaticObjectMethodA(javaClass, methodId, jniArgs);
                if (checkAndClearException(env)) {
                    return QVariantList();
                }
                return Converter::convertJavaListToQVariantList(javaValue);
            } else if constexpr (std::is_same_v<ReturnType, QStringList>) {
                const auto javaValue = env->CallStaticObjectMethodA(javaClass, methodId, jniArgs);
                if (checkAndClearException(env)) {
                    return QStringList();
                }
                return Converter::convertJavaListToQStringList(javaValue);
            } else {
                // everything else (URI, ...)
                const auto result = env->CallStaticObjectMethodA(javaClass, methodId, jniArgs);
                checkAndClearException(env);
                return result;
            }
        }

    private:
        template<typename ReturnType>
        static auto invokeGetter(JNIEnv *env, const jobject javaObject, const jmethodID getter)
        {
            if constexpr (std::is_same_v<ReturnType, jint>) {
                const auto result = env->CallIntMethod(javaObject, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jboolean>) {
                const auto result = env->CallBooleanMethod(javaObject, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, bool>) {
                const auto result = env->CallBooleanMethod(javaObject, getter);
                checkAndClearException(env);
                return static_cast<bool>(result);
            } else if constexpr (std::is_same_v<ReturnType, jfloat>) {
                const auto result = env->CallFloatMethod(javaObject, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jdouble>) {
                const auto result = env->CallDoubleMethod(javaObject, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jlong>) {
                const auto result = env->CallLongMethod(javaObject, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, qint64>) {
                const auto result = env->CallLongMethod(javaObject, getter);
                checkAndClearException(env);
                return static_cast<qint64>(result);
            } else if constexpr (std::is_same_v<ReturnType, jshort>) {
                const auto result = env->CallShortMethod(javaObject, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jbyte>) {
                const auto result = env->CallByteMethod(javaObject, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jchar>) {
                const auto result = env->CallCharMethod(javaObject, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, QChar>) {
                const auto result = env->CallCharMethod(javaObject, getter);
                checkAndClearException(env);
                return QChar(result);
            } else if constexpr (std::is_same_v<ReturnType, jobject>) {
                const auto result = env->CallObjectMethod(javaObject, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jstring>) {
                const auto result = static_cast<jstring>(env->CallObjectMethod(javaObject, getter));
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, QString>) {
                const auto jstr = static_cast<jstring>(env->CallObjectMethod(javaObject, getter));
                if (checkAndClearException(env)) {
                    return QString();
                }
                return toQString(jstr);
            } else if constexpr (std::is_same_v<ReturnType, QVariant>) {
                const auto javaValue = env->CallObjectMethod(javaObject, getter);
                if (checkAndClearException(env)) {
                    return QVariant();
                }
                return Converter::convertObjectToQVariant(javaValue);
            } else if constexpr (std::is_same_v<ReturnType, QVariantList>) {
                const auto javaValue = env->CallObjectMethod(javaObject, getter);
                if (checkAndClearException(env)) {
                    return QVariantList();
                }
                return Converter::convertJavaListToQVariantList(javaValue);
            } else if constexpr (std::is_same_v<ReturnType, QStringList>) {
                const auto javaValue = env->CallObjectMethod(javaObject, getter);
                if (checkAndClearException(env)) {
                    return QStringList();
                }
                return Converter::convertJavaListToQStringList(javaValue);
            } else {
                // Everything else (URI, ...)
                const auto result = env->CallObjectMethod(javaObject, getter);
                checkAndClearException(env);
                return result;
            }
        }

     template<typename ReturnType>
        static auto invokeStaticGetter(JNIEnv *env, const jclass javaClass, const jmethodID getter)
        {
            if constexpr (std::is_same_v<ReturnType, jint>) {
                const auto result = env->CallStaticIntMethod(javaClass, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jboolean>) {
                const auto result = env->CallStaticBooleanMethod(javaClass, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, bool>) {
                const auto result = env->CallStaticBooleanMethod(javaClass, getter);
                checkAndClearException(env);
                return static_cast<bool>(result);
            } else if constexpr (std::is_same_v<ReturnType, jfloat>) {
                const auto result = env->CallStaticFloatMethod(javaClass, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jdouble>) {
                const auto result = env->CallStaticDoubleMethod(javaClass, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jlong>) {
                const auto result = env->CallStaticLongMethod(javaClass, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, qint64>) {
                const auto result = env->CallStaticLongMethod(javaClass, getter);
                checkAndClearException(env);
                return static_cast<qint64>(result);
            } else if constexpr (std::is_same_v<ReturnType, jshort>) {
                const auto result = env->CallStaticShortMethod(javaClass, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jbyte>) {
                const auto result = env->CallStaticByteMethod(javaClass, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jchar>) {
                const auto result = env->CallStaticCharMethod(javaClass, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, QChar>) {
                const auto result = env->CallStaticCharMethod(javaClass, getter);
                checkAndClearException(env);
                return QChar(result);
            } else if constexpr (std::is_same_v<ReturnType, jobject>) {
                const auto result = env->CallStaticObjectMethod(javaClass, getter);
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, jstring>) {
                const auto result = static_cast<jstring>(env->CallStaticObjectMethod(javaClass, getter));
                checkAndClearException(env);
                return result;
            } else if constexpr (std::is_same_v<ReturnType, QString>) {
                const auto jstr = static_cast<jstring>(env->CallStaticObjectMethod(javaClass, getter));
                if (checkAndClearException(env))
                    return QString();
                return toQString(jstr);
            } else if constexpr (std::is_same_v<ReturnType, QVariant>) {
                const auto javaValue = env->CallStaticObjectMethod(javaClass, getter);
                checkAndClearException(env);
                return Converter::convertObjectToQVariant(javaValue);
            } else if constexpr (std::is_same_v<ReturnType, QVariantList>) {
                const auto javaValue = env->CallStaticObjectMethod(javaClass, getter);
                checkAndClearException(env);
                return Converter::convertJavaListToQVariantList(javaValue);
            } else if constexpr (std::is_same_v<ReturnType, QStringList>) {
                const auto javaValue = env->CallStaticObjectMethod(javaClass, getter);
                checkAndClearException(env);
                return Converter::convertJavaListToQStringList(javaValue);
            } else {
                // Everything else
                const auto result = env->CallStaticObjectMethod(javaClass, getter);
                checkAndClearException(env);
                return result;
            }
        }
    };
    template<typename ReturnType>
    auto JNIMethodInvoker::invokeMethodWithJValues(JNIEnv *env, const jobject javaObject,
                                                   const jmethodID methodId, const jvalue *jniArgs)
    {
        if constexpr (std::is_same_v<ReturnType, void>) {
            env->CallVoidMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
        } else if constexpr (std::is_same_v<ReturnType, jint>) {
            const auto result = env->CallIntMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return result;
        } else if constexpr (std::is_same_v<ReturnType, jboolean>) {
            const auto result = env->CallBooleanMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return result;
        } else if constexpr (std::is_same_v<ReturnType, bool>) {
            const auto result = env->CallBooleanMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return static_cast<bool>(result);
        } else if constexpr (std::is_same_v<ReturnType, jfloat>) {
            const auto result = env->CallFloatMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return result;
        } else if constexpr (std::is_same_v<ReturnType, jdouble>) {
            const auto result = env->CallDoubleMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return result;
        } else if constexpr (std::is_same_v<ReturnType, jlong>) {
            const auto result = env->CallLongMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return result;
        } else if constexpr (std::is_same_v<ReturnType, qint64>) {
            const auto result = env->CallLongMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return static_cast<qint64>(result);
        } else if constexpr (std::is_same_v<ReturnType, jshort>) {
            auto result = env->CallShortMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return result;
        } else if constexpr (std::is_same_v<ReturnType, jbyte>) {
            const auto result = env->CallByteMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return result;
        } else if constexpr (std::is_same_v<ReturnType, jchar>) {
            const auto result = env->CallCharMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return result;
        } else if constexpr (std::is_same_v<ReturnType, QChar>) {
            const auto result = env->CallCharMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return QChar(result);
        } else if constexpr (std::is_same_v<ReturnType, jobject>) {
            auto result = env->CallObjectMethodA(javaObject, methodId, jniArgs);
            checkAndClearException(env);
            return result;
        } else if constexpr (std::is_same_v<ReturnType, jstring>) {
            const auto result = static_cast<jstring>(env->CallObjectMethodA(javaObject, methodId, jniArgs));
            checkAndClearException(env);
            return result;
        } else if constexpr (std::is_same_v<ReturnType, QString>) {
            const auto jstr = static_cast<jstring>(env->CallObjectMethodA(javaObject, methodId, jniArgs));
            if (checkAndClearException(env)) {
                return QString();
            }
            return toQString(jstr);
        } else if constexpr (std::is_same_v<ReturnType, QVariant>) {
            const auto javaValue = env->CallObjectMethodA(javaObject, methodId, jniArgs);
            if (checkAndClearException(env)) {
                return QVariant();
            }
            return Converter::convertObjectToQVariant(javaValue);
        } else if constexpr (std::is_same_v<ReturnType, QVariantMap>) {
            const auto javaValue = env->CallObjectMethodA(javaObject, methodId, jniArgs);
            if (checkAndClearException(env))
                return QVariantMap();
            return Converter::convertJavaMapToQVariantMap(javaValue);
        } else if constexpr (std::is_same_v<ReturnType, QVariantList>) {
            const auto javaValue = env->CallObjectMethodA(javaObject, methodId, jniArgs);
            if (checkAndClearException(env)) {
                return QVariantList();
            }
            return Converter::convertJavaListToQVariantList(javaValue);
        } else if constexpr (std::is_same_v<ReturnType, QStringList>) {
            const auto javaValue = env->CallObjectMethodA(javaObject, methodId, jniArgs);
            if (checkAndClearException(env)) {
                return QStringList();
            }
            return Converter::convertJavaListToQStringList(javaValue);
        }
    }
} // namespace Utility::JNI
#endif // JNI_METHOD_INVOKER_H
