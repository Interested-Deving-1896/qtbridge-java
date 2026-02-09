/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#include "jni_context.h"

#include <QtCore/qdebug.h>
#include <QtCore/qloggingcategory.h>

Q_DECLARE_LOGGING_CATEGORY(QT_BRIDGE)

JavaVM *JniContext::javaVM = nullptr;

void JniContext::setJavaVM(JavaVM *vm) { javaVM = vm; }

JavaVM *JniContext::getJavaVM() { return javaVM; }

JNIEnv *JniContext::getEnv()
{
    if (!javaVM) {
        qCCritical(QT_BRIDGE, "JavaVM is not set");
        return nullptr;
    }

    JNIEnv *env = nullptr;
    jint result = javaVM->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    if (result == JNI_EDETACHED) {
        if (javaVM->AttachCurrentThread(reinterpret_cast<void **>(&env), nullptr) != 0) {
            qCCritical(QT_BRIDGE, "Failed to attach current thread to JVM");
            return nullptr;
        }
    } else if (result != JNI_OK) {
        qCCritical(QT_BRIDGE, "Failed to get JNIEnv from JavaVM.");
        return nullptr;
    }

    return env;
}
