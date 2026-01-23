/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#ifndef JAVA_OBJECT_H
#define JAVA_OBJECT_H

#include "jni_cache.h"
#include "jni_method_invoker.h"
#include "signature_helper.h"

#include <jni.h>

namespace Utility::JNI {
    class JavaObject
    {
    public:
        template<typename... Args>
        static jobject newInstanceWithSignature(JNIEnv *env, const jclass clazz,
                                                const char *signature, Args... args)
        {
            const auto ctor = env->GetMethodID(clazz, "<init>", signature);
            if (!ctor) {
                qDebug() << "Constructor for class with signature" << signature << "not found";
                return {};
            }

            if constexpr (sizeof...(args) == 0) {
                return env->NewObject(clazz, ctor);
            } else {
                const jvalue jniArgs[] = {wrapJValue(env, args)...};
                return env->NewObjectA(clazz, ctor, jniArgs);
            }
        }

        template<typename ReturnType, typename... Args>
        static auto callMethod(jweak weakRef, const char* method, Args&&... args)
        {
            JNIEnv* env = JniContext::getEnv();
            jobject localRef = weakRef ? env->NewLocalRef(weakRef) : nullptr;
            if (!localRef) {
                qWarning() << "Object already garbage collected";
                return;
            }
            jclass clazz = env->GetObjectClass(localRef);
            JavaObject::callMethod<ReturnType>(env, clazz, localRef, method, std::forward<Args>(args)...);
            env->DeleteLocalRef(clazz);
            env->DeleteLocalRef(localRef);
        }

        template<typename ReturnType, typename... Args>
        static auto callMethod(JNIEnv *env, const jclass clazz, const jobject javaObject,
                                     const char *methodName, Args... args)
        {
            constexpr auto sigData = SignatureHelper::buildMethodJniSignature<ReturnType, Args...>();
            const auto signature = QString::fromUtf8(sigData, sigData.size());
            const auto methodId = env->GetMethodID(clazz, methodName, signature.toUtf8().constData());
            Q_ASSERT_X(methodId, "Method lookup",
                       qUtf8Printable(QStringLiteral("Method %1 with signature %2 not found")
                               .arg(QString::fromLatin1(methodName)).arg(signature)));
            return JNIMethodInvoker::invokeMethod<ReturnType>(env, javaObject, methodId, args...);
        }

        template<typename ReturnType, typename... Args>
        static auto callMethod(JNIEnv *env, const char *className, jobject javaObject,
                               const char *methodName, Args... args)
        {
            constexpr auto sigData = SignatureHelper::buildMethodJniSignature<ReturnType, Args...>();
            const QString signature = QString::fromUtf8(sigData, sigData.size());

            const QByteArray classNameBytes(className);
            const QByteArray methodNameBytes(methodName);
            const QByteArray signatureBytes = signature.toUtf8();

            const auto globalMethod = JNICache::getGlobalMethod(classNameBytes, methodNameBytes, signatureBytes);
            const QString assertMessage = QStringLiteral("Method not found: %1 with signature %2 in class %3")
                            .arg(QString::fromLatin1(methodName), signature, QString::fromLatin1(className));
            Q_ASSERT_X(globalMethod.method, "JNI method lookup", assertMessage.toUtf8().constData());
            return JNIMethodInvoker::invokeMethod<ReturnType>(env, javaObject, globalMethod.method, args...);
        }

        template<typename ReturnType, typename... Args>
        static auto callStaticMethod(JNIEnv *env, jclass javaClass, const char *methodName, Args... args)
        {
            constexpr auto sigData = SignatureHelper::buildMethodJniSignature<ReturnType, Args...>();
            const auto signature = QString::fromUtf8(sigData, sigData.size());
            const auto methodId = env->GetStaticMethodID(javaClass, methodName, signature.toUtf8().constData());
            Q_ASSERT_X(methodId, "Static method lookup",
                       qUtf8Printable(QStringLiteral("Static method %1 with signature %2 not found")
                               .arg(QString::fromLatin1(methodName)).arg(signature)));
            return JNIMethodInvoker::invokeStaticMethod<ReturnType>(env, javaClass, methodId, args...);
        }

        template<typename ReturnType, typename... Args>
        static auto callStaticMethod(JNIEnv *env, const char *className, jclass javaClass,
                                     const char *methodName, Args... args)
        {
            constexpr auto sigData = SignatureHelper::buildMethodJniSignature<ReturnType, Args...>();
            const QString signature = QString::fromUtf8(sigData, sigData.size());

            const QByteArray classNameBytes(className);
            const QByteArray methodNameBytes(methodName);
            const QByteArray signatureBytes = signature.toUtf8();

            const auto globalMethod = JNICache::getGlobalMethod(classNameBytes, methodNameBytes,
                                                                signatureBytes, true);

            const QString assertMessage = QStringLiteral("Static Method not found: %1 with signature %2 in class %3")
                            .arg(QString::fromLatin1(methodName), signature, QString::fromLatin1(className));
            Q_ASSERT_X(globalMethod.method, "JNI method lookup", assertMessage.toUtf8().constData());
            return JNIMethodInvoker::invokeStaticMethod<ReturnType>(env, javaClass, globalMethod.method, args...);
        }

        template<typename PropertyType>
        static PropertyType callGetter(JNIEnv *env, const jobject javaObject, const char *getterName)
        {
            const auto clazz = env->GetObjectClass(javaObject);
            const auto result = JavaObject::callMethod<PropertyType>(env, clazz, javaObject, getterName);
            env->DeleteLocalRef(clazz);
            return result;
        }

        template<typename PropertyType>
        static void callSetter(JNIEnv *env, jobject javaObject, const char *setterName, PropertyType value)
        {
            const auto clazz = env->GetObjectClass(javaObject);
            JavaObject::callMethod<void>(env, clazz, javaObject, setterName, value);
            env->DeleteLocalRef(clazz);
        }

        template<typename PropertyType>
        static PropertyType getProperty(JNIEnv *env, const jobject javaObject, const char *propertyName)
        {
            const auto name = QString::fromUtf8(propertyName);
            const auto getterName = QStringLiteral("get%1%2").arg(name[0].toUpper()).arg(name.mid(1));
            const auto clazz = env->GetObjectClass(javaObject);
            const auto result = JavaObject::callMethod<PropertyType>(
                    env, clazz, javaObject, getterName.toUtf8().constData());
            env->DeleteLocalRef(clazz);
            return result;
        }

        template<typename PropertyType>
        static void setProperty(JNIEnv *env, jobject javaObject, const char *propertyName, PropertyType value)
        {
            const auto name = QString::fromUtf8(propertyName);
            const auto setterName = QStringLiteral("set%1%2").arg(name[0].toUpper()).arg(name.mid(1));
            const auto clazz = env->GetObjectClass(javaObject);
            JavaObject::callMethod<void>(env, clazz, javaObject, setterName.toUtf8().constData(), value);
            env->DeleteLocalRef(clazz);
        }
    };
} // namespace Utility::JNI

#endif
