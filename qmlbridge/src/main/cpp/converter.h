/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */
#ifndef CONVERTER_H
#define CONVERTER_H

#include "jni_context.h"
#include "jni_cache.h"

#include <QtCore/qmetaobject.h>
#include <QtCore/qstring.h>
#include <QtCore/qvariantlist.h>
#include <QtCore/qvariantmap.h>

#include <jni.h>

namespace Utility::JNI {
        inline QString toQString(const jstring str)
        {
            if (!str)
                return {};
            const int32_t strLength = JniContext::getEnv()->GetStringLength(str);
            QString res(strLength, Qt::Uninitialized);
            JniContext::getEnv()->GetStringRegion(str, 0, strLength, reinterpret_cast<jchar *>(res.data()));
            return res;
        }

        class Converter
        {
        public:
            static jvalue cppParameterToJavaParameter(
                JNIEnv *env, const QMetaMethod& metaMethod,
                void *cppParameter, const JNICache::JMethodEntry methodEntry,
                int parameterIndex);
            static bool javaParameterToCppParameter(int cppMetaTypeId, JNIEnv *env, jobject valueObj, void *outPtr);

            static QVariantMap convertEnumToQVariantMap(const jobject &javaObject);
            static QVariant convertObjectToQVariant(const jobject &javaObject);
            static QVariantMap convertJavaMapToQVariantMap(const jobject &jmap);
            static QVariantList convertJavaListToQVariantList(const jobject &javaObject);
            static QStringList convertJavaToQStringList(const jobject &javaObject);

            static jobject convertQVariantToObject(const QVariant &var);
            static jobject convertQVariantListToObject(const QVariantList &value);
            static jobject convertQVariantMapToObject(const QVariantMap &value);
            static jobject convertQStringListToObject(const QStringList &value);
        };
} // namespace Utility::JNI
#endif // CONVERTER_H
