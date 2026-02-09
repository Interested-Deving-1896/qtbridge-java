/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#ifndef JNI_OBJECT_H
#define JNI_OBJECT_H

#include "java_object.h"
#include "jni_cache.h"

#include <QtCore/qloggingcategory.h>

#include <jni.h>

Q_DECLARE_LOGGING_CATEGORY(QT_BRIDGE)

template<typename Tag>
class JNIObject
{
    static bool checkClassRegistered()
    {
        if (!JNICache::isGlobalClassRegistered(Tag::className())) {
            qCDebug(QT_BRIDGE) << "Class " << Tag::className()
                     << " not registered. Call registerClass() first.";
            return false;
        }
        return true;
    }

public:
    static void registerClass(JNIEnv *env)
    {
        const auto local = env->FindClass(Tag::className());
        if (local != nullptr) {
            JNICache::registerGlobalClass(Tag::className(), local);
            env->DeleteLocalRef(local);
        }
    }

    static jclass get()
    {
        return JNICache::getGlobalClass(Tag::className());
    }

    static void release(JNIEnv *env)
    {
        JNICache::unregisterGlobalClass(Tag::className());
    }

    template<typename... Args>
    static jobject newInstanceWithSignature(const char *signature, Args &&...args)
    {
        Q_ASSERT(checkClassRegistered());
        return Utility::JNI::JavaObject::newInstanceWithSignature(
                JniContext::getEnv(), get(), signature, std::forward<Args>(args)...);
    }

    template<typename... Args>
    static jobject makeObject(Args &&...args)
    {
        Q_ASSERT(Tag::constructorSignature() != nullptr);
        return newInstanceWithSignature(Tag::constructorSignature(), std::forward<Args>(args)...);
    }

    template<typename ReturnType, typename... Args>
    static auto callMethod(jobject obj, const char *methodName, Args &&...args)
    {
        Q_ASSERT(checkClassRegistered());
        return Utility::JNI::JavaObject::callMethod<ReturnType>(
                JniContext::getEnv(), Tag::className(), obj, methodName, std::forward<Args>(args)...);
    }

    template<typename ReturnType, typename... Args>
    static auto callStaticMethod(const char *methodName, Args &&...args)
    {
        Q_ASSERT(checkClassRegistered());
        return Utility::JNI::JavaObject::callStaticMethod<ReturnType>(
                JniContext::getEnv(),Tag::className(), get(), methodName, std::forward<Args>(args)...);
    }

    static bool isInstanceOf(const jobject obj)
    {
        Q_ASSERT(checkClassRegistered());
        return JniContext::getEnv()->IsInstanceOf(obj, get());
    }

    static bool is(const jclass clazz)
    {
        Q_ASSERT(checkClassRegistered());
        return JniContext::getEnv()->IsAssignableFrom(clazz, get());
    }
};

#endif // JNI_OBJECT_H
