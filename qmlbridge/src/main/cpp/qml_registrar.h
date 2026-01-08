// Copyright (C) 2025 The Qt Company Ltd.
// SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only

#ifndef QML_REGISTRAR_H
#define QML_REGISTRAR_H

#include <jni.h>
// QmlRegistrar class is used to:
// - register QML types (singletons and instantiable types) at application startup
// - instantiate QML types when QML asks to do so
class QmlRegistrar
{
public:
    static void initializeJNI(JNIEnv *env);
    static void clearRegistrations();
};

#endif // QML_REGISTRAR_H
