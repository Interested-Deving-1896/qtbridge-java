/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */
#ifndef JNI_QTOBJECT_H
#define JNI_QTOBJECT_H

#include <jni.h>

class JNIQtObject
{
public:
    static void initializeJNI(JNIEnv *env);
};
#endif
