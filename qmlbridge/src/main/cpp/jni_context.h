/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#ifndef JNI_CONTEXT_H
#define JNI_CONTEXT_H

#include <jni.h>

class JniContext
{
public:
    static void setJavaVM(JavaVM *vm);
    static JavaVM *getJavaVM();
    static JNIEnv *getEnv(); // Returns env for the current thread (attaches if needed)

private:
    static JavaVM *javaVM;
};

#endif // JNI_CONTEXT_H
