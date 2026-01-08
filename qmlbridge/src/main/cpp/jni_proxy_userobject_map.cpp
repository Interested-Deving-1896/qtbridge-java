// Copyright (C) 2025 The Qt Company Ltd.
// SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only

#include "jni_proxy_userobject_map.h"

#include "jni_method_invoker.h"
#include "jni_object.h"
#include "jni_context.h"
#include "jni_type.h"

#include <QtCore/qhash.h>
#include <QtCore/qmutex.h>

using namespace Utility::JNI;

// One entry maps a Java user object (weak global) to its native proxy.
struct UserProxyEntry {
    jweak userWeakRef = nullptr;          // weak global ref to user object
    QObjectJavaProxy* proxy = nullptr;    // native proxy pointer
};

struct UserProxyRegistry {
    QMutex mutex;
    QHash<jint, QList<UserProxyEntry>> buckets;

    // Returns System.identityHashCode for the given obj. This can be used as a
    // hash key. The values may (rarely) collide, but for that we disambiguate
    // per key bucket. In all likelihood there is ever just one item per key bucket,
    // but better to be prepared as the bug could be very difficult to find.
    static jint identityHash(JNIEnv* env, jobject obj)
    {
        static jclass systemClass = nullptr;
        static jmethodID methodId = nullptr;
        if (!systemClass) {
            jclass local = env->FindClass("java/lang/System");
            checkAndClearException(env);

            if (local) {
                systemClass = static_cast<jclass>(env->NewGlobalRef(local));
                checkAndClearException(env);
                env->DeleteLocalRef(local);
            }
        }
        if (systemClass && !methodId)
            methodId = env->GetStaticMethodID(systemClass, "identityHashCode", "(Ljava/lang/Object;)I");
        if (!systemClass || !methodId)
            return 0; // fallback; all such keys will collide but we still disambiguate
        const auto ret = env->CallStaticIntMethod(systemClass, methodId, obj);
        checkAndClearException(env);
        return ret;
    }

    void addMapping(jobject userObj, QObjectJavaProxy* proxy)
    {
        if (!userObj || !proxy)
            return;

        JNIEnv* env = JniContext::getEnv();
        QMutexLocker lock(&mutex);
        auto &bucket = buckets[identityHash(env, userObj)];

        // Find if an entry already exists, and update if it does
        for (int i = 0; i < bucket.size();) {
            auto &entry = bucket[i];
            jobject localRef = env->NewLocalRef(entry.userWeakRef);
            // Prune the bucket if the userObject pointed by a proxy is gone (GC'd)
            if (!localRef) {
                if (entry.userWeakRef)
                    env->DeleteWeakGlobalRef(entry.userWeakRef);
                bucket.removeAt(i);
                continue;
            }
            // Check if the pointed object is the same (we can't compare reference values)
            const bool isSame = env->IsSameObject(userObj, localRef);
            env->DeleteLocalRef(localRef);
            if (isSame) {
                entry.proxy = proxy;
                return;
            }
            ++i;
        }

        // Create new entry
        UserProxyEntry entry;
        entry.userWeakRef = env->NewWeakGlobalRef(userObj);
        entry.proxy = proxy;
        bucket.push_back(entry);
    }

    // Returns the proxy for 'userObject'
    QObjectJavaProxy* getProxy(jobject userObject)
    {
        if (!userObject)
            return nullptr;
        JNIEnv* env = JniContext::getEnv();
        QMutexLocker lock(&mutex);

        // Find the right bucket (list of proxies)
        auto it = buckets.find(identityHash(env, userObject));
        if (it == buckets.end())
            return nullptr;

        // Find the right proxy in that list.
        auto &bucket = it.value();
        for (int i = 0; i < bucket.size();) {
            auto &entry = bucket[i];
            jobject localRef = env->NewLocalRef(entry.userWeakRef);
            // Prune the bucket if the userObject pointed by a proxy is gone (GC'd)
            if (!localRef) {
                if (entry.userWeakRef)
                    env->DeleteWeakGlobalRef(entry.userWeakRef);
                bucket.removeAt(i);
                continue;
            }
            // Check if the pointed object is the same (we can't compare reference values)
            const bool isSame = env->IsSameObject(userObject, localRef);
            env->DeleteLocalRef(localRef);
            if (isSame)
                return entry.proxy;
            ++i;
        }
        // No match found. Remove the bucket if it became empty
        if (bucket.isEmpty())
            buckets.erase(it);
        return nullptr;
    }

    // Creates a new proxy if one didn't exist
    QObjectJavaProxy* ensureProxy(JNIEnv *env, jobject userObject, bool ownedByQML)
    {
        if (!env || !userObject)
            return nullptr;

        QObjectJavaProxy *proxy = getProxy(userObject);
        if (proxy)
            return proxy;

        jobject qtObjectLocal = nullptr;
        jclass userClassLocalRef = nullptr;
        auto guard = qScopeGuard([&](){
            if (qtObjectLocal)
                env->DeleteLocalRef(qtObjectLocal);
            if (userClassLocalRef)
                env->DeleteLocalRef(userClassLocalRef);
        });

        jclass qtObjClass = JNIObject<JavaQtObject>::get();
        jmethodID ctor = env->GetMethodID(qtObjClass, "<init>", "(Ljava/lang/Object;JZ)V");
        checkAndClearException(env);
        if (!ctor) {
            qWarning("ensureProxy(): Unable to find proxy constructor");
            return nullptr;
        }

        // Get cache key for the class
        userClassLocalRef = env->GetObjectClass(userObject);
        checkAndClearException(env);
        if (!userClassLocalRef) {
            qWarning("ensureProxy(): Failed to get user class");
            return nullptr;
        }
        proxy = new QObjectJavaProxy(JNICache::ensureProxyClass(userClassLocalRef));
        qtObjectLocal = env->NewObject(qtObjClass, ctor, userObject,
                                       jlong(proxy), jboolean(ownedByQML));
        checkAndClearException(env);
        if (!qtObjectLocal) {
            qWarning("ensureProxy(): Creating a proxy for property failed");
            delete proxy;
            return nullptr;
        }
        proxy->registerUserObject(userObject, ownedByQML);
        proxy->registerQtObject(qtObjectLocal);
        return proxy;
    }

    // Removes mappings that 'proxy' has
    void removeByProxy(QObjectJavaProxy* proxy)
    {
        if (!proxy)
            return;
        JNIEnv* env = JniContext::getEnv();
        QMutexLocker lock(&mutex);

        for (auto it = buckets.begin(); it != buckets.end(); ) {
            auto &bucket = it.value();
            for (int i = 0; i < bucket.size(); ) {
                if (bucket[i].proxy == proxy) {
                    if (bucket[i].userWeakRef)
                        env->DeleteWeakGlobalRef(bucket[i].userWeakRef);
                    bucket.removeAt(i);
                } else {
                    ++i;
                }
            }
            if (bucket.isEmpty())
                it = buckets.erase(it);
            else
                ++it;
        }
    }
};

Q_GLOBAL_STATIC(UserProxyRegistry, s_userProxyRegistry)

void JNIProxyUserObjectMap::addMapping(jobject userObject, QObjectJavaProxy* proxy)
{
    if (s_userProxyRegistry)
        s_userProxyRegistry->addMapping(userObject, proxy);
}

QObjectJavaProxy* JNIProxyUserObjectMap::getProxy(jobject userObject)
{
    return s_userProxyRegistry ? s_userProxyRegistry->getProxy(userObject) : nullptr;
}

QObjectJavaProxy* JNIProxyUserObjectMap::ensureProxy(JNIEnv *env, jobject userObject, bool ownedByQML)
{
    return s_userProxyRegistry ? s_userProxyRegistry->ensureProxy(env, userObject, ownedByQML) : nullptr;
}

void JNIProxyUserObjectMap::removeByProxy(QObjectJavaProxy* proxy)
{
    if (s_userProxyRegistry)
        s_userProxyRegistry->removeByProxy(proxy);
}

