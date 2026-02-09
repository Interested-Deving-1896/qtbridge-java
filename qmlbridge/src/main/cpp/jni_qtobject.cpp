/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#include "converter.h"
#include "jni_cache.h"
#include "jni_object.h"
#include "jni_qtobject.h"
#include "jni_type.h"
#include "jni_utilities.h"
#include "qobject_java_proxy.h"

#include <QtCore/qloggingcategory.h>

using namespace Utility;

Q_DECLARE_LOGGING_CATEGORY(QT_BRIDGE)

void JNICALL nativeDisposeQObjectJavaProxy(JNIEnv, jclass, jlong handle)
{
    auto *obj = QObjectJavaProxy::fromHandle(handle);
    // Use deleteLater because native-side may be disposed in an
    // arbitrary Java Cleaner thread
    obj->deleteLater();
}

void JNICALL nativeAddInvokable(JNIEnv *env, jobject, jlong handle, jstring javaSignature,
                                jstring javaReturnType, jstring cppSignature,
                                jstring cppReturnType, jboolean retIsPrimitive,
                                jbooleanArray paramIsPrimitive)
{
    auto *obj = QObjectJavaProxy::fromHandle(handle);
    if (!obj)
        return;

    const auto jSignature = JNI::toQString(javaSignature);
    const auto jReturnType = JNI::toQString(javaReturnType);

    const auto cSignature = JNI::toQString(cppSignature);
    const auto cReturnType = JNI::toQString(cppReturnType);

    QList<bool> jParamIsPrimitive;
    if (paramIsPrimitive) {
        jsize count = env->GetArrayLength(paramIsPrimitive);
        jboolean *primElems = env->GetBooleanArrayElements(paramIsPrimitive, nullptr);
        for (jsize i = 0; i < count; ++i)
            jParamIsPrimitive.push_back(primElems[i] == JNI_TRUE);
        env->ReleaseBooleanArrayElements(paramIsPrimitive, primElems, 0);
    }

    const auto slotId = obj->addSlot(cSignature.toUtf8(), cReturnType.toUtf8());
    JNICache::registerProxyMethod(obj->cacheKey(), slotId, jSignature,
                                  jReturnType, bool(retIsPrimitive), jParamIsPrimitive);
}

void JNICALL nativeAddSignal(JNIEnv *env, jobject, jlong handle, jstring javaSignature,
                             jstring cppSignature, jobjectArray cppParamTypes)
{
    const auto jSignature = JNI::toQString(javaSignature);
    const auto cSignature = JNI::toQString(cppSignature).toUtf8();
    const auto *obj = QObjectJavaProxy::fromHandle(handle);
    if (!obj) {
        qCWarning(QT_BRIDGE, "No object found for signal %s", qPrintable(jSignature));
        return;
    }

    // Collect param C++ type strings from String[]
    QList<QByteArray> paramCppTypes;
    if (cppParamTypes) {
        jsize count = env->GetArrayLength(cppParamTypes);
        paramCppTypes.reserve(count);
        for (jsize i = 0; i < count; ++i) {
            jstring typeString = static_cast<jstring>(env->GetObjectArrayElement(cppParamTypes, i));
            const auto qtTypeString = JNI::toQString(typeString).toUtf8();
            env->DeleteLocalRef(typeString);
            paramCppTypes.push_back(qtTypeString);
        }
    }

    const auto signalIndex = obj->addSignal(cSignature);
    JNICache::registerProxySignal(obj->cacheKey(), signalIndex, jSignature, paramCppTypes);
}

void JNICALL nativeEmitSignal(JNIEnv *env, jobject, jlong handle,
                              jstring javaSignature, jobjectArray args)
{
    auto *qtObject = QObjectJavaProxy::fromHandle(handle);
    const auto jSignature = JNI::toQString(javaSignature).toUtf8();
    if (!qtObject) {
        qCWarning(QT_BRIDGE, "Proxy object not found for signal: %s", jSignature.constData());
        return;
    }
    const auto signalCacheEntry = JNICache::getProxySignal(qtObject->cacheKey(), jSignature);
    if (!signalCacheEntry) {
        qCWarning(QT_BRIDGE, "Cache entry not found for signal: %s", jSignature.constData());
        return;
    }

    const auto signalIndex = signalCacheEntry->signalIndex;
    const jsize argCount = args ? env->GetArrayLength(args) : 0;
    const auto &metaIds = signalCacheEntry->parmMetaTypeIds;

    if (argCount != metaIds.size()) {
        qCWarning(QT_BRIDGE, "Signal argument count mismatch for %s", jSignature.constData());
        return;
    }

    // Small RAII helper to free memory we've allocated
    struct MetaArgs {
        // QMetaType ID of each argument
        QVarLengthArray<int, 4>   metaIds;
        // Return value + arguments to pass for QMetaObject::metacall()
        QVarLengthArray<void*, 4> retAndArgPtrs;

        explicit MetaArgs(jsize argCount) {
            metaIds.reserve(argCount);
            retAndArgPtrs.reserve(argCount + 1);
            retAndArgPtrs.append(nullptr); // return value at [0] (unused with signals)
        }
        ~MetaArgs() {
            // Free starting at index '1' to skip over the return value
            for (int i = 1; i < retAndArgPtrs.size(); ++i)
                QMetaType(metaIds[i - 1]).destroy(retAndArgPtrs[i]);
            metaIds.clear();
            retAndArgPtrs.clear();
        }

        MetaArgs(MetaArgs&& o) noexcept
            : metaIds(std::move(o.metaIds))
            , retAndArgPtrs(std::move(o.retAndArgPtrs))
        {
            // Make the moved-from do nothing in its dtor
            o.metaIds.clear();
            o.retAndArgPtrs.clear();
        }
        // Don't accidentally copy the helper - move only
        MetaArgs(const MetaArgs&) = delete;
        MetaArgs& operator=(const MetaArgs&) = delete;
    };

    MetaArgs mArgs(argCount);

    // Convert each Java argument to a C++ value
    for (int i = 0; i < argCount; ++i) {
        jobject arg = env->GetObjectArrayElement(args, i);
        const auto guard = qScopeGuard([&](){ env->DeleteLocalRef(arg); });

        const int metaId = metaIds.at(i);
        const QMetaType mt(metaId);

        // 1. Default-construct currently processed signal argument
        void *p = mt.create();
        if (!p) {
            qCWarning(QT_BRIDGE, "Failed to allocate signal arg %s", jSignature.constData());
            return;
        }

        // 2. Assign it with a proper value
        if (!JNI::Converter::javaParameterToCppParameter(metaId, env, arg, p)) {
            qCWarning(QT_BRIDGE, "Failed to convert signal arg %s", jSignature.constData());
            return;
        }

        // 3. Add argument to argument list
        mArgs.retAndArgPtrs.append(p);
        mArgs.metaIds.append(metaId);
    }

    // Emit the signal
    QMetaObject::invokeMethod(
        qtObject, [qtObject, signalIndex, m = std::move(mArgs) ]() mutable {
            QMetaObject::metacall(qtObject, QMetaObject::InvokeMetaMethod,
                                  signalIndex, m.retAndArgPtrs.data());
        }, Qt::QueuedConnection);
}

void JNICALL nativeAddProperty(JNIEnv, jobject, jlong handle, jstring name,
                               jstring javaType, jstring cppType, jboolean writable,
                               jboolean readable,jstring signalSignature,
                               jboolean isConstant)
{
    auto *proxy = QObjectJavaProxy::fromHandle(handle);
    if (!proxy) {
        qCWarning(QT_BRIDGE) << "Proxy not found for property:" << name;
        return;
    }

    const auto propertyName = JNI::toQString(name);
    const auto propertyJavaType = JNI::toQString(javaType);
    auto propertyCppType = JNI::toQString(cppType);
    const auto notifySig = JNI::toQString(signalSignature);

    if (propertyCppType.isEmpty()) {
        qCWarning(QT_BRIDGE, "Property cppType is empty %s", qPrintable(propertyName));
        return;
    }
    const auto fieldId = proxy->addProperty(propertyName.toUtf8(),
                                    {
                                        propertyCppType.toUtf8(), static_cast<bool>(writable),
                                        static_cast<bool>(readable), static_cast<bool>(isConstant),
                                        notifySig.toUtf8()
                                    });
    JNICache::registerProxyField(proxy->cacheKey(), fieldId, propertyName, propertyJavaType);
}

void JNIQtObject::initializeJNI(JNIEnv *env)
{
    static JNINativeMethod methods[] = {
        JNIUtilities::createJNIMethod("nativeDispose", "(J)V",
                                      (void *)&nativeDisposeQObjectJavaProxy),
        JNIUtilities::createJNIMethod("nativeAddInvokable",
                                      "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z[Z)V",
                                      (void *)&nativeAddInvokable),
        JNIUtilities::createJNIMethod("nativeAddSignal", "(JLjava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V",
                                      (void *)&nativeAddSignal),
        JNIUtilities::createJNIMethod("nativeAddProperty",
                                   "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;Z)V",
                                   (void *)&nativeAddProperty),
        JNIUtilities::createJNIMethod("nativeEmitSignal",
                                      "(JLjava/lang/String;[Ljava/lang/Object;)V",
                                      (void *)&nativeEmitSignal),
    };
    const auto javaClass = JNIObject<JavaQtObject>::get();
    env->RegisterNatives(javaClass, methods, std::size(methods));
}
