/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#ifndef JNI_UTILITIES_H
#define JNI_UTILITIES_H

#include <jni.h>

class JNIUtilities
{
public:
    static JNINativeMethod createJNIMethod(const char *name, const char *sig, void *funcPtr);
    static void throwIllegalStateException(const char *msg);
    static jlong getNativeHandleFromObject(jobject qtObject);
};
#endif
