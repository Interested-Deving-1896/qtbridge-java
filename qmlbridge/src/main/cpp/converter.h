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

        // Must be kept in synch with ClassCreationEmitter table
        enum class VarShape : qint8 {
            Value = 0,
            List = 1,
            Array = 2,
            Map = 3,
        };

        // Must be kept in synch with ClassCreationEmitter table
        enum class VarType : qint8 {
            Void = 0,
            Boolean = 1,
            Byte = 2,
            Char = 3,
            Short = 4,
            Int = 5,
            Long = 6,
            Float = 7,
            Double = 8,
            String = 9,
            QmlRegistrable = 10,
            ItemModel = 11
        };

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
            static QStringList convertJavaListToQStringList(const jobject &javaObject);
            static QVariantList convertJavaArrayToQVariantList(JNIEnv *env, jobject javaArray, qint8 elemType);
            static QStringList convertJavaArrayToQStringList(JNIEnv *env, jobject javaArray, qint8 elemType);

            static jobject convertQVariantToObject(const QVariant &var);
            static jobject convertQVariantListToObject(const QVariantList &value);
            static jobject convertQVariantListToArray(JNIEnv *env, const QVariantList &list,
                                                      qint8 elemType);
            static jobject convertQVariantMapToObject(const QVariantMap &value);
            static jobject convertQStringListToObject(const QStringList &value);
            static jobject convertQStringListToArray(JNIEnv *env, const QStringList &list);
        };
} // namespace Utility::JNI
#endif // CONVERTER_H
