/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#include "jni_utilities.h"

#include "jni_object.h"
#include "jni_type.h"
#include "jni_qtlistmodel.h"
#include "jni_qtobject.h"
#include "jni_qml_application.h"
#include "jni_qmlchildren.h"
#include "qml_registrar.h"

#include <QtCore/qloggingcategory.h>

Q_DECLARE_LOGGING_CATEGORY(QT_BRIDGE)

void registerJavaTypes(JNIEnv *env)
{
    JNIObject<JavaLangClass>::registerClass(env);
    JNIObject<JavaLangReflectField>::registerClass(env);
    JNIObject<JavaLangEnum>::registerClass(env);
    JNIObject<JavaLangObject>::registerClass(env);

    JNIObject<JavaLangString>::registerClass(env);
    JNIObject<JavaLangInteger>::registerClass(env);
    JNIObject<JavaLangDouble>::registerClass(env);
    JNIObject<JavaLangLong>::registerClass(env);
    JNIObject<JavaLangFloat>::registerClass(env);
    JNIObject<JavaLangBoolean>::registerClass(env);
    JNIObject<JavaLangByte>::registerClass(env);
    JNIObject<JavaLangCharacter>::registerClass(env);
    JNIObject<JavaLangShort>::registerClass(env);

    JNIObject<JavaList>::registerClass(env);
    JNIObject<JavaArrayList>::registerClass(env);
    JNIObject<JavaMap>::registerClass(env);
    JNIObject<JavaSet>::registerClass(env);
    JNIObject<JavaIterator>::registerClass(env);
    JNIObject<JavaMapEntry>::registerClass(env);
    JNIObject<JavaHashMap>::registerClass(env);

    JNIObject<JavaNetURI>::registerClass(env);

    JNIObject<JavaQtObject>::registerClass(env);
    JNIObject<JavaQtProperty>::registerClass(env);
    JNIObject<JavaQtListModel>::registerClass(env);
    JNIObject<JavaQtQmlRegistration>::registerClass(env);
    JNIObject<JavaQtQmlChildren>::registerClass(env);

    JNIObject<JavaIllegalStateException>::registerClass(env);
}

void releaseJavaTypes(JNIEnv *env)
{
    JNIObject<JavaLangClass>::release(env);
    JNIObject<JavaLangReflectField>::release(env);
    JNIObject<JavaLangEnum>::release(env);
    JNIObject<JavaLangObject>::release(env);

    JNIObject<JavaLangString>::release(env);
    JNIObject<JavaLangInteger>::release(env);
    JNIObject<JavaLangDouble>::release(env);
    JNIObject<JavaLangLong>::release(env);
    JNIObject<JavaLangFloat>::release(env);
    JNIObject<JavaLangBoolean>::release(env);
    JNIObject<JavaLangByte>::release(env);
    JNIObject<JavaLangCharacter>::release(env);
    JNIObject<JavaLangShort>::release(env);

    JNIObject<JavaList>::release(env);
    JNIObject<JavaArrayList>::release(env);
    JNIObject<JavaMap>::release(env);
    JNIObject<JavaSet>::release(env);
    JNIObject<JavaIterator>::release(env);
    JNIObject<JavaMapEntry>::release(env);
    JNIObject<JavaHashMap>::release(env);

    JNIObject<JavaNetURI>::release(env);

    JNIObject<JavaQtObject>::release(env);
    JNIObject<JavaQtProperty>::release(env);
    JNIObject<JavaQtListModel>::release(env);
    JNIObject<JavaQtQmlRegistration>::release(env);
    JNIObject<JavaQtQmlChildren>::release(env);

    JNIObject<JavaIllegalStateException>::release(env);
}

jint JNI_OnLoad(JavaVM *vm, void *)
{
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    JniContext::setJavaVM(vm);
    registerJavaTypes(env);

    QMLApplication::initializeJNI(env);
    JNIQtObject::initializeJNI(env);
    JNIQtListModel::initializeJNI(env);
    QmlRegistrar::initializeJNI(env);
    JNIQmlChildren::initializeJNI(env);

    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *)
{
    JNIEnv *env;
    vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    JNICache::clear();
    releaseJavaTypes(env);
}


void JNIUtilities::throwIllegalStateException(const char *msg)
{
    JniContext::getEnv()->ThrowNew(JNIObject<JavaIllegalStateException>::get(), msg);
}

JNINativeMethod JNIUtilities::createJNIMethod(const char *name, const char *sig, void *funcPtr)
{
    JNINativeMethod method;
    method.name = strdup(name);
    method.signature = strdup(sig);
    method.fnPtr = funcPtr;
    return method;
}


jlong JNIUtilities::getNativeHandleFromObject(jobject object)
{
    if (!object) {
        qCWarning(QT_BRIDGE, "qtObject is null, unable to get native handle");
        return 0;
    }
    const auto env = JniContext::getEnv();
    const auto _class = env->GetObjectClass(object);
    const auto nativePtrFieldID = env->GetFieldID(_class, "nativeHandle", "J");
    if (nativePtrFieldID == nullptr) {
        qCWarning(QT_BRIDGE, "nativeHandle field not found on a qtObject");
        return 0;
    }
    return env->GetLongField(object, nativePtrFieldID);
}
