// Copyright (C) 2025 The Qt Company Ltd.
// SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only

#ifndef JNI_PROXY_USEROBJECT_MAP_H
#define JNI_PROXY_USEROBJECT_MAP_H

#include "qobject_java_proxy.h"

#include <QtCore/qhash.h>

#include <jni.h>

// Helper class that maintains a mapping between user object and proxy.
// This is needed for example when QML needs to read a QMLRegistrable property.
// At the time of the read we have just the property meta 'id' referring to
// a QtProperty. But that QtProperty will contain the userObject, whereas
// QML needs the proxy object. Since userObject is not anyhow aware of its
// proxy counterpart, we need to maintain this mapping elsewhere (here) in
// order to be able to return the proxy-corresponding-to-userObject for QML.
class JNIProxyUserObjectMap
{
public:
    static void addMapping(jobject userObject, QObjectJavaProxy* proxy);
    // Returns proxy if one already existed
    static QObjectJavaProxy *getProxy(jobject userObject);
    // Creates a new proxy if one didn't exist yet. For proxies that it creates,
    // it'll set the ownership as instructed
    static QObjectJavaProxy *ensureProxy(JNIEnv *env, jobject userObject, bool ownedByQML);
    static void removeByProxy(QObjectJavaProxy* proxy);
};

#endif // JNI_PROXY_USEROBJECT_MAP_H
