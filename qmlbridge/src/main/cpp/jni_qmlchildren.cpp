/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#include "jni_qmlchildren.h"

#include "jni_utilities.h"
#include "jni_method_invoker.h"
#include "jni_object.h"
#include "jni_proxy_userobject_map.h"
#include "jni_type.h"

#include <QtCore/qobject.h>

using namespace Utility::JNI;

jobject JNICALL nativeQmlChildren(JNIEnv *env, jclass,
                                  jobject userObject /* parent */, jclass filter)
{
    const jobject result = JNIObject<JavaArrayList>::makeObject();

    // Get the proxy for the user object
    auto *proxy = JNIProxyUserObjectMap::getProxy(userObject);
    if (!proxy) {
        qWarning() << "No proxy found for parent, cannot look up children";
        return result;
    }

    const auto allChildren = static_cast<QObject*>(proxy)->children();
    for (QObject *child : allChildren) {
        // Check we got a QMLRegistrable class. We use the base class
        // QtProxyBase for checking this because qobject_cast<> does not
        // work reliably with dynamic metaobjects (at least across Qt
        // versions).
        auto proxyBase = qobject_cast<QtProxyBase*>(child);
        if (!proxyBase)
            continue;

        jobject userObjectLocalRef = proxyBase->userObjectLocalRef();
        if (!userObjectLocalRef)
            continue;

        if (!filter || (filter && env->IsInstanceOf(userObjectLocalRef, filter))) {
            JNIObject<JavaList>::callMethod<jboolean>(result, "add", userObjectLocalRef);
            checkAndClearException(env);
        }
        env->DeleteLocalRef(userObjectLocalRef);
    }
    return result;
}

void JNIQmlChildren::initializeJNI(JNIEnv *env)
{
    static JNINativeMethod methods[] = {
        JNIUtilities::createJNIMethod("nativeQmlChildren",
                                      "(Ljava/lang/Object;Ljava/lang/Class;)Ljava/util/List;",
                                      (void *)&nativeQmlChildren),
    };
    const auto javaClass = JNIObject<JavaQtQmlChildren>::get();
    env->RegisterNatives(javaClass, methods, std::size(methods));
}
