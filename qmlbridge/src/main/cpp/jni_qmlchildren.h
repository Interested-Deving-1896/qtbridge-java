/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */
#ifndef JNI_QMLCHILDREN_H
#define JNI_QMLCHILDREN_H

#include <jni.h>

class JNIQmlChildren
{
public:
    static void initializeJNI(JNIEnv *env);
};
#endif // JNI_QMLCHILDREN_H
