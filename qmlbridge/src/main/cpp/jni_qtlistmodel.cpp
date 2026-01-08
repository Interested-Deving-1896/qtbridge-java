/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#include "jni_qtlistmodel.h"

#include "converter.h"
#include "jni_utilities.h"
#include "qtlistmodel.h"

#include <QtCore/qdebug.h>

jlong JNICALL nativeCreate(JNIEnv *env, jobject obj)
{
    auto *model = new QtListModel(env->NewWeakGlobalRef(obj));
    return reinterpret_cast<jlong>(model);
}

void JNICALL nativeDisposeListModel(JNIEnv *env, jclass, jlong handle)
{
    auto *obj = reinterpret_cast<QtListModel *>(handle);
    delete obj;
}

jint JNICALL nativeSize(JNIEnv *env, jobject, jlong handle)
{
    auto *model = reinterpret_cast<QtListModel *>(handle);
    return model->size();
}

jboolean JNICALL nativeContainsItem(JNIEnv *env, jobject obj, jlong ptr, jobject jmap)
{
    // Convert itemMap to QVariantMap
    const QVariantMap item = Utility::JNI::Converter::convertJavaMapToQVariantMap(jmap);
    auto *model = reinterpret_cast<QtListModel *>(ptr);
    return model->contains(item);
}

void JNICALL nativeUpdateItemAt(JNIEnv *env, jobject obj, jlong nativePtr, jint index, jobject jmap)
{
    auto *model = reinterpret_cast<QtListModel *>(nativePtr);
    auto item = Utility::JNI::Converter::convertJavaMapToQVariantMap(jmap);
    model->updateItemAt(index, item);
}

void JNICALL nativeAppendItem(JNIEnv *env, jobject obj, jlong nativePtr, jobject jmap)
{
    auto *model = reinterpret_cast<QtListModel *>(nativePtr);
    QVariantMap item = Utility::JNI::Converter::convertJavaMapToQVariantMap(jmap);
    model->appendItem(item);
}

void JNICALL nativeRemoveItemAt(JNIEnv *env, jobject obj, jlong nativePtr, jint index)
{
    auto *model = reinterpret_cast<QtListModel *>(nativePtr);
    model->removeItemAt(index);
}

void JNICALL nativeRemoveItem(JNIEnv *env, jobject obj, jlong nativePtr, jobject jmap)
{
    auto *model = reinterpret_cast<QtListModel *>(nativePtr);
    QVariantMap item = Utility::JNI::Converter::convertJavaMapToQVariantMap(jmap);
    model->removeItem(item);
}

void JNICALL nativeReset(JNIEnv *env, jobject obj, jlong nativePtr)
{
    auto *model = reinterpret_cast<QtListModel *>(nativePtr);
    model->reset();
}

void JNICALL nativeReplaceAll(JNIEnv *env, jobject /*obj*/, jlong nativePtr, jobject jlist)
{
    auto *model = reinterpret_cast<QtListModel *>(nativePtr);
    if (!model)
        return;

    // Convert Java List<Map<String, Object>> -> QList<QVariantMap>
    QList<QVariantMap> items;
    if (jlist) {
        jclass listClass = env->GetObjectClass(jlist);
        if (listClass) {
            jmethodID sizeMethodId = env->GetMethodID(listClass, "size", "()I");
            jmethodID getMethodId  = env->GetMethodID(listClass, "get", "(I)Ljava/lang/Object;");
            if (sizeMethodId && getMethodId) {
                const jint sz = env->CallIntMethod(jlist, sizeMethodId);
                if (!env->ExceptionCheck()) {
                    items.reserve(sz);
                    for (jint i = 0; i < sz; ++i) {
                        jobject elem = env->CallObjectMethod(jlist, getMethodId, i);
                        if (env->ExceptionCheck()) {
                            env->ExceptionDescribe();
                            env->ExceptionClear();
                            break;
                        }
                        if (elem) {
                            QVariantMap map =
                                Utility::JNI::Converter::convertJavaMapToQVariantMap(elem);
                            items.append(map);
                            env->DeleteLocalRef(elem);
                        }
                    }
                } else {
                    env->ExceptionDescribe();
                    env->ExceptionClear();
                }
            }
            env->DeleteLocalRef(listClass);
        }
    }
    model->replaceAll(items);
}

void JNIQtListModel::initializeJNI(JNIEnv *env)
{
    static JNINativeMethod methods[] = {
        JNIUtilities::createJNIMethod("nativeCreate", "()J", (void *)&nativeCreate),
        JNIUtilities::createJNIMethod("nativeDispose", "(J)V", (void *)&nativeDisposeListModel),
        JNIUtilities::createJNIMethod("nativeSize", "(J)I", (void *)&nativeSize),
        JNIUtilities::createJNIMethod("nativeContains", "(JLjava/util/Map;)Z",
                                      (void *)&nativeContainsItem),
        JNIUtilities::createJNIMethod("nativeAppendItem", "(JLjava/util/Map;)V",
                                      (void *)&nativeAppendItem),
        JNIUtilities::createJNIMethod("nativeRemoveItem", "(JLjava/util/Map;)V",
                                      (void *)&nativeRemoveItem),
        JNIUtilities::createJNIMethod("nativeRemoveItemAt", "(JI)V", (void *)&nativeRemoveItemAt),
        JNIUtilities::createJNIMethod("nativeUpdateItemAt", "(JILjava/util/Map;)V",
                                      (void *)&nativeUpdateItemAt),
        JNIUtilities::createJNIMethod("nativeReset", "(J)V", (void *)&nativeReset),
        JNIUtilities::createJNIMethod("nativeReplaceAll", "(JLjava/util/List;)V",
                                      (void *)&nativeReplaceAll),
    };
    jclass javaClass = env->FindClass("org/qtproject/qt/bridge/core/QtListModel");
    env->RegisterNatives(javaClass, methods, std::size(methods));
    env->DeleteLocalRef(javaClass);
}
