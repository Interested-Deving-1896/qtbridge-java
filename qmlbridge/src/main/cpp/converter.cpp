/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#include "converter.h"

#include "jni_object.h"
#include "jni_proxy_userobject_map.h"
#include "jni_type.h"

#include <QtCore/qloggingcategory.h>
#include <QtCore/qmap.h>
#include <QtCore/qmetatype.h>
#include <QtCore/qstring.h>
#include <QtCore/qurl.h>
#include <QtCore/qvariantlist.h>
#include <QtCore/qvariantmap.h>

#include <QtQml/qjsvalue.h>

using namespace Qt::StringLiterals;

Q_DECLARE_LOGGING_CATEGORY(QT_BRIDGE)

namespace Utility::JNI {
    // Used to convert invokable function parameters from cpp to Java.
    // Target Java types may be boxed or unboxed. To know which is expected,
    // we use the cache entry helper that was created at KSP time
    jvalue Converter::cppParameterToJavaParameter(
        JNIEnv *env, const QMetaMethod& metaMethod, void *cppParameter,
        const JNICache::JMethodEntry methodEntry, int parameterIndex)
    {
        Q_ASSERT(methodEntry.parmType.size() > parameterIndex);

        jvalue ret{};
        const auto paramMetaTypeId = metaMethod.parameterMetaType(parameterIndex).id();

        switch (paramMetaTypeId) {
        case QMetaType::QVariant: {
            QVariant &qv = *static_cast<QVariant *>(cppParameter);
            ret.l = convertQVariantToObject(qv);
            break;
        }
        case QMetaType::QVariantMap: {
            QVariantMap &qmap = *static_cast<QVariantMap *>(cppParameter);
            ret.l = convertQVariantMapToObject(qmap);
            break;
        }
        case QMetaType::QVariantList: {
            QVariantList &qlist = *static_cast<QVariantList *>(cppParameter);
            // Check if Java/Kotlin-side expects an array or a List
            if (static_cast<VariableShape>(methodEntry.parmShape.at(parameterIndex)) == VariableShape::Array)
                ret.l = convertQVariantListToArray(env, qlist, methodEntry.parmType.at(parameterIndex));
            else
                ret.l = convertQVariantListToObject(qlist);
            break;
        }
        case QMetaType::QStringList: {
            QStringList &slist = *static_cast<QStringList *>(cppParameter);
            if (static_cast<VariableShape>(methodEntry.parmShape.at(parameterIndex)) == VariableShape::Array)
                ret.l = convertQStringListToArray(env, slist);
            else
                ret.l = convertQStringListToObject(slist);
            break;
        }
        case QMetaType::QUrl: {
            QUrl &qurl = *static_cast<QUrl *>(cppParameter);
            ret.l = JNIObject<JavaNetURI>::makeObject(qurl.toString());
            break;
        }
        case QMetaType::QString: {
            QString &qs = *static_cast<QString *>(cppParameter);
            ret.l = JNIObject<JavaLangString>::makeObject(qs);
            break;
        }
        case QMetaType::LongLong: {
            qint64 cppValue = *static_cast<long long*>(cppParameter);
            if (typeIsPrimitive(methodEntry.parmType.at(parameterIndex)))
                ret.j = cppValue;
            else
                ret.l = JNIObject<JavaLangLong>::makeObject(jlong(cppValue));
            break;
        }
        case QMetaType::Double: {
            double cppValue = *static_cast<double *>(cppParameter);
            if (typeIsPrimitive(methodEntry.parmType.at(parameterIndex)))
                ret.d = cppValue;
            else
                ret.l = JNIObject<JavaLangDouble>::makeObject(jdouble(cppValue));
            break;
        }
        case QMetaType::Float: {
            float cppValue = *static_cast<float *>(cppParameter);
            if (typeIsPrimitive(methodEntry.parmType.at(parameterIndex)))
                ret.f = cppValue;
            else
                ret.l = JNIObject<JavaLangFloat>::makeObject(jfloat(cppValue));
            break;

        }
        case QMetaType::Bool: {
            bool cppValue = *static_cast<bool *>(cppParameter);
            if (typeIsPrimitive(methodEntry.parmType.at(parameterIndex)))
                ret.z = cppValue;
            else
                ret.l = JNIObject<JavaLangBoolean>::makeObject(jboolean(cppValue));
            break;
        }
        case QMetaType::QChar: {
            // Both Java Character and QChar are UTF-16 characters
            QChar cppValue = *static_cast<QChar *>(cppParameter);
            const jchar jc = static_cast<jchar>(cppValue.unicode());
            if (typeIsPrimitive(methodEntry.parmType.at(parameterIndex)))
                ret.c = jc;
            else
                ret.l = JNIObject<JavaLangCharacter>::makeObject(jc);
            break;
        }
        case QMetaType::QObjectStar: {
            // QML passed QObjectJavaProxy* pointer. We need change it to the
            // corresponding user-side Java object
            QObject *qobj = *static_cast<QObject * const *>(cppParameter);
            auto *proxy = qobject_cast<QtProxyBase*>(qobj);
            if (!proxy) {
                qCWarning(QT_BRIDGE, "A non-proxy QObject* as parameter to %s",
                          metaMethod.name().constData());
            }
            ret.l = proxy ? proxy->userObjectLocalRef() : nullptr;
            break;
        }
        case QMetaType::SChar: {
            // QMetaType::SChar == signed char == qint8 == jbyte
            signed char cppValue = *static_cast<signed char *>(cppParameter);
            if (typeIsPrimitive(methodEntry.parmType.at(parameterIndex)))
                ret.b = cppValue;
            else
                ret.l = JNIObject<JavaLangByte>::makeObject(jbyte(cppValue));
            break;
        }
        case QMetaType::Int: {
            int cppValue = *static_cast<int *>(cppParameter);
            if (typeIsPrimitive(methodEntry.parmType.at(parameterIndex)))
                ret.i = cppValue;
            else
                ret.l = JNIObject<JavaLangInteger>::makeObject(jint(cppValue));
            break;
        }
        case QMetaType::Short: {
            short cppValue = *static_cast<short *>(cppParameter);
            if (typeIsPrimitive(methodEntry.parmType.at(parameterIndex)))
                ret.s = cppValue;
            else
                ret.l = JNIObject<JavaLangShort>::makeObject(jshort(cppValue));
            break;
        }
        default:
            qCWarning(QT_BRIDGE, "Unsupported invokable parameter metatype %i in %s",
                     paramMetaTypeId, metaMethod.methodSignature().constData());
            break;
        };
        return ret;
    }

    // Used to convert signal parameters. Signal parameters are boxed because
    // they go through the signal proxy which auto-boxes them. Java arrays
    // such as int[] themselves may contain boxed and unboxed values though
    bool Converter::javaParameterToCppParameter(JNIEnv *env,
                                                int cppMetaTypeId, jobject valueObj,
                                                qint8 shape, qint8 type, void *outPtr)
    {
        Q_ASSERT(outPtr);

        if (!valueObj) {
            // Leave the default-constructed value produced by QMetaType::create()
            // in the caller. This makes Java 'null' map to a sensible C++ default
            // (e.g. empty QString, empty QVariantMap, nullptr for QObject*, etc.).
            return true;
        }

        const auto varShape = static_cast<VariableShape>(shape);

        switch (cppMetaTypeId) {
        case QMetaType::QVariant: {
            *static_cast<QVariant *>(outPtr) = convertObjectToQVariant(valueObj);
            return true;
        }
        case QMetaType::QVariantList:
            if (varShape == VariableShape::Array) {
                *static_cast<QVariantList *>(outPtr) = convertJavaArrayToQVariantList(env, valueObj, type);
                return true;
            } else {
                *static_cast<QVariantList *>(outPtr) = convertJavaListToQVariantList(valueObj);
                return true;

            }
        case QMetaType::QStringList:
            if (varShape == VariableShape::Array) {
                *static_cast<QStringList *>(outPtr) = convertJavaArrayToQStringList(env, valueObj);
                return true;
            } else {
                *static_cast<QStringList *>(outPtr) = convertJavaListToQStringList(valueObj);
                return true;
            }
        case QMetaType::LongLong: {
            const auto value = JNIMethodInvoker::invokeMethod<jlong>(
                env, valueObj, JNICache::javaLongValueMethod());
            *static_cast<long long *>(outPtr) = value;
            return true;
        }
        case QMetaType::QString: {
            *static_cast<QString *>(outPtr) = Utility::JNI::toQString(jstring(valueObj));
            return true;
        }
        case QMetaType::Float: {
            const auto value = JNIMethodInvoker::invokeMethod<jfloat>(env, valueObj, JNICache::javaFloatValueMethod());
            *static_cast<float *>(outPtr) = value;
            return true;
        }
        case QMetaType::Bool: {
            const auto value = JNIMethodInvoker::invokeMethod<jboolean>(env, valueObj, JNICache::javaBooleanValueMethod());
            *static_cast<bool *>(outPtr) = value;
            return true;
        }
        case QMetaType::QChar: {
            const auto value = JNIMethodInvoker::invokeMethod<jchar>(env, valueObj, JNICache::javaCharValueMethod());
            QChar qc(value);
            *static_cast<QChar *>(outPtr) = qc;
            return true;
        }
        case QMetaType::SChar: {
            // QMetaType::SChar == signed char == qint8 == jbyte
            const auto value = JNIMethodInvoker::invokeMethod<jbyte>(env, valueObj, JNICache::javaByteValueMethod());
            *static_cast<signed char *>(outPtr) = value;
            return true;
        }
        case QMetaType::Short: {
            const auto value = JNIMethodInvoker::invokeMethod<jshort>(env, valueObj, JNICache::javaShortValueMethod());
            *static_cast<short *>(outPtr) = value;
            return true;
        }
        case QMetaType::Int: {
            const auto value = JNIMethodInvoker::invokeMethod<jint>(env, valueObj, JNICache::javaIntValueMethod());
            *static_cast<int *>(outPtr) = value;
            return true;
        }
        case QMetaType::Double: {
            const auto value = JNIMethodInvoker::invokeMethod<jdouble>(env, valueObj, JNICache::javaDoubleValueMethod());
            *static_cast<double *>(outPtr) = value;
            return true;
        }
        case QMetaType::QUrl: {
            const auto value = JNIObject<JavaNetURI>::callMethod<QString>(valueObj, "toString");
            *static_cast<QUrl *>(outPtr) = QUrl(value);
            return true;
        }
        case QMetaType::QObjectStar: {
            const auto value = JNIProxyUserObjectMap::ensureProxy(env, valueObj, false);
            *static_cast<QObject **>(outPtr) = value;
            return true;
        }
        case QMetaType::QVariantMap: {
            QVariantMap map;
            if (varType(type) == VariableType::Enum)
                map = convertEnumToQVariantMap(valueObj);
            else
                map = convertJavaMapToQVariantMap(valueObj);
            *static_cast<QVariantMap *>(outPtr) = std::move(map);
            return true;
        }
        default:
            qCWarning(QT_BRIDGE, "Unsupported signal parameter metaId: %i", cppMetaTypeId);
        }

        return false;
    }

    QVariantMap Converter::convertEnumToQVariantMap(const jobject &javaObject)
    {
        QVariantMap result;
        const auto name = JNIObject<JavaLangEnum>::callMethod<QString>(javaObject, "name");
        const auto ordinal = JNIObject<JavaLangEnum>::callMethod<jint>(javaObject, "ordinal");
        result["name"_L1] = name;
        result["ordinal"_L1] = int(ordinal);

        const auto env = JniContext::getEnv();
        const auto _declaringCls =
                JNIObject<JavaLangEnum>::callMethod<jclass>(javaObject, "getDeclaringClass");
        const auto _fields = static_cast<jobjectArray>(
            JNIObject<JavaLangClass>::callMethod<JNIArray<JavaLangReflectField>>(_declaringCls, "getDeclaredFields")
        );
        const auto _fieldCount = env->GetArrayLength(_fields);
        for (auto i = 0; i < _fieldCount; ++i) {
            const auto _fieldObj = env->GetObjectArrayElement(_fields, i);
            const auto _isEnumConstant =
                    JNIObject<JavaLangReflectField>::callMethod<bool>(_fieldObj, "isEnumConstant");
            const auto _isSynthetic =
                    JNIObject<JavaLangReflectField>::callMethod<bool>(_fieldObj, "isSynthetic");
            if (_isEnumConstant || _isSynthetic) {
                env->DeleteLocalRef(_fieldObj);
                continue;
            }
            JNIObject<JavaLangReflectField>::callMethod<void>(_fieldObj, "setAccessible", true);
            const auto _getValue = env->GetMethodID(JNIObject<JavaLangReflectField>::get(), "get",
                                        "(Ljava/lang/Object;)Ljava/lang/Object;");
            checkAndClearException(env);
            const auto _fieldValue = env->CallObjectMethod(_fieldObj, _getValue, javaObject);
            checkAndClearException(env);
            if (_fieldValue) {
                const auto _fieldName =
                        JNIObject<JavaLangReflectField>::callMethod<QString>(_fieldObj, "getName");
                result[_fieldName] = convertObjectToQVariant(_fieldValue);
                env->DeleteLocalRef(_fieldValue);
            }
            env->DeleteLocalRef(_fieldObj);
        }
        env->DeleteLocalRef(_fields);
        env->DeleteLocalRef(_declaringCls);
        return result;
    }

    QVariant Converter::convertObjectToQVariant(const jobject &javaObject)
    {
        JNIEnv *env = JniContext::getEnv();

        if (javaObject == nullptr) {
            // If the object is null, set a proper Javascript null value (instead of undefined)
            return QVariant::fromValue(QJSValue(QJSValue::NullValue));
        }

        if (env->IsInstanceOf(javaObject, JNIObject<JavaLangString>::get()))
            return Utility::JNI::toQString(static_cast<jstring>(javaObject));

        if (env->IsInstanceOf(javaObject, JNIObject<JavaLangInteger>::get()))
            return int(JNIMethodInvoker::invokeMethod<jint>(env, javaObject, JNICache::javaIntValueMethod()));
        if (env->IsInstanceOf(javaObject, JNIObject<JavaLangBoolean>::get()))
            return bool(JNIMethodInvoker::invokeMethod<jboolean>(env, javaObject, JNICache::javaBooleanValueMethod()));
        if (env->IsInstanceOf(javaObject, JNIObject<JavaLangDouble>::get()))
            return double(JNIMethodInvoker::invokeMethod<jdouble>(env, javaObject, JNICache::javaDoubleValueMethod()));
        if (env->IsInstanceOf(javaObject, JNIObject<JavaLangFloat>::get()))
            return float(JNIMethodInvoker::invokeMethod<jfloat>(env, javaObject, JNICache::javaFloatValueMethod()));
        if (env->IsInstanceOf(javaObject, JNIObject<JavaLangLong>::get()))
            return qint64(JNIMethodInvoker::invokeMethod<jlong>(env, javaObject, JNICache::javaLongValueMethod()));
        if (env->IsInstanceOf(javaObject, JNIObject<JavaLangByte>::get()))
            return qint8(JNIMethodInvoker::invokeMethod<jbyte>(env, javaObject, JNICache::javaByteValueMethod()));
        if (env->IsInstanceOf(javaObject, JNIObject<JavaLangShort>::get()))
            return short(JNIMethodInvoker::invokeMethod<jshort>(env, javaObject, JNICache::javaShortValueMethod()));
        if (env->IsInstanceOf(javaObject, JNIObject<JavaLangCharacter>::get()))
            return QChar(JNIMethodInvoker::invokeMethod<jchar>(env, javaObject, JNICache::javaCharValueMethod()));

        if (env->IsInstanceOf(javaObject, JNIObject<JavaLangEnum>::get()))
            return convertEnumToQVariantMap(javaObject);

        if (env->IsInstanceOf(javaObject, JNIObject<JavaList>::get()))
           return convertJavaListToQVariantList(javaObject);

        if (env->IsInstanceOf(javaObject, JNIObject<JavaMap>::get()))
            return convertJavaMapToQVariantMap(javaObject);

        if (env->IsInstanceOf(javaObject, JNIObject<JavaNetURI>::get())) {
            const auto s = JNIObject<JavaNetURI>::callMethod<QString>(javaObject, "toString");
            return QUrl{s};
        }
        // Keep Object check last, as it's the most generic one
        if (env->IsInstanceOf(javaObject, JNIObject<JavaLangObject>::get())) {
            QObject *proxy = JNIProxyUserObjectMap::ensureProxy(env, javaObject, false);
            return QVariant::fromValue(proxy);
        }

        qCWarning(QT_BRIDGE, "Unsupported Java type for QVariant conversion");
        return {};
    }

    QVariantMap Converter::convertJavaMapToQVariantMap(const jobject &jmap)
    {
        JNIEnv *env = JniContext::getEnv();
        const auto mapSize =
            JNIMethodInvoker::invokeMethod<jint>(env, jmap, JNICache::javaMapSizeMethod());
        if (mapSize == 0)
            return {};

        const auto entrySet =
            JNIMethodInvoker::invokeMethod<jobject>(env, jmap, JNICache::javaMapEntrySetMethod());
        const auto iterator =
            JNIMethodInvoker::invokeMethod<jobject>(env, entrySet, JNICache::javaSetIteratorMethod());

        QVariantMap qtMap;
        for (jint i = 0; i < mapSize; ++i) {
            const auto entry =
                JNIMethodInvoker::invokeMethod<jobject>(env, iterator, JNICache::javaIteratorNextMethod());
            const auto key =
                JNIMethodInvoker::invokeMethod<jobject>(env, entry, JNICache::javaMapEntryGetKeyMethod());
            const auto value =
                JNIMethodInvoker::invokeMethod<jobject>(env, entry, JNICache::javaMapEntryGetValueMethod());

            QString qtKey;
            if (key && env->IsInstanceOf(key, JNIObject<JavaLangString>::get()))
                qtKey = Utility::JNI::toQString(static_cast<jstring>(key));
            else
                qtKey = convertObjectToQVariant(key).toString();

            const auto qtValue = convertObjectToQVariant(value);
            qtMap.insert(qtKey, qtValue);
            env->DeleteLocalRef(value);
            env->DeleteLocalRef(key);
            env->DeleteLocalRef(entry);
        }
        env->DeleteLocalRef(iterator);
        env->DeleteLocalRef(entrySet);
        return qtMap;
    }

    QVariantList Converter::convertJavaListToQVariantList(const jobject &javaObject)
    {
        JNIEnv *env = JniContext::getEnv();
        const auto size =
            JNIMethodInvoker::invokeMethod<jint>(env, javaObject, JNICache::javaListSizeMethod());
        QVariantList qtList;
        qtList.reserve(size);
        for (jint i = 0; i < size; ++i) {
            const auto element =
                JNIMethodInvoker::invokeMethod<jobject>(env, javaObject, JNICache::javaListGetMethod(), i);
            qtList.append(convertObjectToQVariant(element));
            if (element)
                env->DeleteLocalRef(element);
        }
        return qtList;
    }

    QStringList Converter::convertJavaListToQStringList(const jobject &javaObject)
    {
        JNIEnv *env = JniContext::getEnv();
        const auto size =
            JNIMethodInvoker::invokeMethod<jint>(env, javaObject, JNICache::javaListSizeMethod());
        QStringList qtList;
        qtList.reserve(size);
        for (jint i = 0; i < size; ++i) {
            const auto element =
                JNIMethodInvoker::invokeMethod<jobject>(env, javaObject, JNICache::javaListGetMethod(), i);
            qtList.append(toQString(static_cast<jstring>(element)));
            if (element)
                env->DeleteLocalRef(element);
        }
        return qtList;
    }

    QVariantList Converter::convertJavaArrayToQVariantList(JNIEnv* env, jobject javaArray, qint8 elemType)
    {
        QVariantList result;
        if (!javaArray)
            return result;

        const VariableType type = varType(elemType);
        const bool isPrimitive = typeIsPrimitive(elemType);

        switch (type) {
        case VariableType::Boolean:
            if (isPrimitive) {
                auto array = static_cast<jbooleanArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                QList<jboolean> tmp(size);
                env->GetBooleanArrayRegion(array, 0, size, tmp.data());
                for (jsize i = 0; i < size; ++i)
                    result.append(bool(tmp.at(i)));
            } else {
                const auto array = static_cast<jobjectArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                for (jsize i = 0; i < size; ++i) {
                    jobject value = env->GetObjectArrayElement(array, i);
                    if (!value) { // Guard for null because callMethod below would throw
                        result.append(QVariant{});
                        continue;
                    }
                    result.append(bool(
                        JNIMethodInvoker::invokeMethod<jboolean>(
                            env, value, JNICache::javaBooleanValueMethod())));
                    env->DeleteLocalRef(value);
                }
            }
            break;
        case VariableType::Byte:
            if (isPrimitive) {
                auto array = static_cast<jbyteArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                QList<jbyte> tmp(size);
                env->GetByteArrayRegion(array, 0, size, tmp.data());
                for (jsize i = 0; i < size; ++i)
                    result.append(qint8(tmp.at(i)));
            } else {
                const auto array = static_cast<jobjectArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                for (jsize i = 0; i < size; ++i) {
                    jobject value = env->GetObjectArrayElement(array, i);
                    if (!value) { // Guard for null because callMethod below would throw
                        result.append(QVariant{});
                        continue;
                    }
                    result.append(qint8(JNIMethodInvoker::invokeMethod<jbyte>(
                        env, value, JNICache::javaByteValueMethod())));
                    env->DeleteLocalRef(value);
                }
            }
            break;
        case VariableType::Char:
            if (isPrimitive) {
                auto array = static_cast<jcharArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                QList<jchar> tmp(size);
                env->GetCharArrayRegion(array, 0, size, tmp.data());
                for (jsize i = 0; i < size; ++i)
                    result.append(QChar(tmp.at(i)));
            } else {
                const auto array = static_cast<jobjectArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                for (jsize i = 0; i < size; ++i) {
                    jobject value = env->GetObjectArrayElement(array, i);
                    if (!value) { // Guard for null because callMethod below would throw
                        result.append(QVariant{});
                        continue;
                    }
                    result.append(
                        QChar(JNIMethodInvoker::invokeMethod<jchar>(
                            env, value, JNICache::javaCharValueMethod())));
                    env->DeleteLocalRef(value);
                }
            }
            break;
        case VariableType::Short:
            if (isPrimitive) {
                auto array = static_cast<jshortArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                QList<jshort> tmp(size);
                env->GetShortArrayRegion(array, 0, size, tmp.data());
                for (jsize i = 0; i < size; ++i)
                    result.append(int(tmp.at(i)));
            } else {
                const auto array = static_cast<jobjectArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                for (jsize i = 0; i < size; ++i) {
                    jobject value = env->GetObjectArrayElement(array, i);
                    if (!value) { // Guard for null because callMethod below would throw
                        result.append(QVariant{});
                        continue;
                    }
                    result.append(int(JNIMethodInvoker::invokeMethod<jshort>(
                        env, value, JNICache::javaShortValueMethod())));
                    env->DeleteLocalRef(value);
                }
            }
            break;
        case VariableType::Int:
            if (isPrimitive) {
                auto array = static_cast<jintArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                QList<jint> tmp(size);
                env->GetIntArrayRegion(array, 0, size, tmp.data());
                for (jsize i = 0; i < size; ++i)
                    result.append(int(tmp.at(i)));
            } else {
                const auto array = static_cast<jobjectArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                for (jsize i = 0; i < size; ++i) {
                    jobject value = env->GetObjectArrayElement(array, i);
                    if (!value) { // Guard for null because callMethod below would throw
                        result.append(QVariant{});
                        continue;
                    }
                    result.append(int(JNIMethodInvoker::invokeMethod<jint>(
                        env, value, JNICache::javaIntValueMethod())));
                    env->DeleteLocalRef(value);
                }
            }
            break;
        case VariableType::Long:
            if (isPrimitive) {
                auto array = static_cast<jlongArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                QList<jlong> tmp(size);
                env->GetLongArrayRegion(array, 0, size, tmp.data());
                for (jsize i = 0; i < size; ++i)
                    result.append(qint64(tmp.at(i)));
            } else {
                const auto array = static_cast<jobjectArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                for (jsize i = 0; i < size; ++i) {
                    jobject value = env->GetObjectArrayElement(array, i);
                    if (!value) { // Guard for null because callMethod below would throw
                        result.append(QVariant{});
                        continue;
                    }
                    result.append(
                        qint64(JNIMethodInvoker::invokeMethod<jlong>(
                            env, value, JNICache::javaLongValueMethod())));
                    env->DeleteLocalRef(value);
                }
            }
            break;
        case VariableType::Float:
            if (isPrimitive) {
                auto array = static_cast<jfloatArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                QList<jfloat> tmp(size);
                env->GetFloatArrayRegion(array, 0, size, tmp.data());
                for (jsize i = 0; i < size; ++i)
                    result.append(float(tmp.at(i)));
            } else {
                const auto array = static_cast<jobjectArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                for (jsize i = 0; i < size; ++i) {
                    jobject value = env->GetObjectArrayElement(array, i);
                    if (!value) { // Guard for null because callMethod below would throw
                        result.append(QVariant{});
                        continue;
                    }
                    result.append(
                        float(JNIMethodInvoker::invokeMethod<jfloat>(
                            env, value, JNICache::javaFloatValueMethod())));
                    env->DeleteLocalRef(value);
                }
            }
            break;
        case VariableType::Double:
            if (isPrimitive) {
                auto array = static_cast<jdoubleArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                QList<jdouble> tmp(size);
                env->GetDoubleArrayRegion(array, 0, size, tmp.data());
                for (jsize i = 0; i < size; ++i)
                    result.append(double(tmp.at(i)));
            } else {
                const auto array = static_cast<jobjectArray>(javaArray);
                const jsize size = env->GetArrayLength(array);
                result.reserve(size);
                for (jsize i = 0; i < size; ++i) {
                    jobject value = env->GetObjectArrayElement(array, i);
                    if (!value) { // Guard for null because callMethod below would throw
                        result.append(QVariant{});
                        continue;
                    }
                    result.append(
                        double(JNIMethodInvoker::invokeMethod<jdouble>(
                            env, value, JNICache::javaDoubleValueMethod())));
                    env->DeleteLocalRef(value);
                }
            }
            break;
        case VariableType::String: {
            const auto array = static_cast<jobjectArray>(javaArray);
            const jsize size = env->GetArrayLength(array);
            result.reserve(size);
            for (jsize i = 0; i < size; ++i) {
                jobject value = env->GetObjectArrayElement(array, i);
                if (!value) {
                    result.append(QString());
                    continue;
                }
                result.append(Utility::JNI::toQString(static_cast<jstring>(value)));
                env->DeleteLocalRef(value);
            }
            break;
        }
        case VariableType::QmlRegistrable: {
            const auto array = static_cast<jobjectArray>(javaArray);
            const jsize size = env->GetArrayLength(array);
            result.reserve(size);
            for (jsize i = 0; i < size; ++i) {
                jobject value = env->GetObjectArrayElement(array, i);
                result.append(convertObjectToQVariant(value));
                if (value)
                    env->DeleteLocalRef(value);
            }
            break;
        }
        default:
            qCWarning(QT_BRIDGE, "Unsupported array element type: %d", int(type));
        }
        return result;
    }

    QStringList Converter::convertJavaArrayToQStringList(JNIEnv *env, jobject javaArray)
    {
        QStringList result;
        if (!javaArray)
            return result;

        const auto array = static_cast<jobjectArray>(javaArray);
        const jsize size = env->GetArrayLength(array);
        result.reserve(size);

        for (jsize i = 0; i < size; ++i) {
            jobject value = env->GetObjectArrayElement(array, i);
            if (!value) {
                result.append(QString());
                continue;
            }
            result.append(Utility::JNI::toQString(static_cast<jstring>(value)));
            env->DeleteLocalRef(value);
        }
        return result;
    }

    jobject Converter::convertQVariantToObject(const QVariant &var)
    {
        if (!var.isValid()) {
            // If the variant value is not valid, convert it to a Java null
            return nullptr;
        }

        switch (var.typeId()) {
            case QMetaType::QUrl: {
                const auto qurl = var.toUrl();
                const auto jstr = JNIObject<JavaLangString>::makeObject(qurl.toString(QUrl::FullyEncoded));
                return JNIObject<JavaNetURI>::callStaticMethod<JavaNetURI>("create", static_cast<jstring>(jstr));
            }
            case QMetaType::Nullptr: return nullptr;
            case QMetaType::Int:     return JNIObject<JavaLangInteger>::makeObject(var.toInt());
            case QMetaType::Double:  return JNIObject<JavaLangDouble>::makeObject(var.toDouble());
            case QMetaType::Bool:    return JNIObject<JavaLangBoolean>::makeObject(var.toBool());
            case QMetaType::Float:   return JNIObject<JavaLangFloat>::makeObject(var.toFloat());
            case QMetaType::QString: return JNIObject<JavaLangString>::makeObject(var.toString());
            case QMetaType::QVariantMap: return convertQVariantMapToObject(var.toMap());
            case QMetaType::QVariantList: return convertQVariantListToObject(var.toList());
            case QMetaType::QStringList: return convertQStringListToObject(var.toStringList());
            case QMetaType::QObjectStar: {
                QtProxyBase *proxy = qobject_cast<QtProxyBase*>(var.value<QObject*>());
                if (!proxy) {
                    qCWarning(QT_BRIDGE, "Invalid proxy object in QVariant");
                    return nullptr;
                }
                return proxy->userObjectLocalRef();
            }

            default:
                // Check QJSValue separately because it's metaId is in the 'user category'
                if (var.userType() == qMetaTypeId<QJSValue>()) {
                    const auto val = var.value<QJSValue>();
                    if (val.isNull() || val.isUndefined()) {
                        return nullptr;
                    } else if (val.isArray()) {
                        return convertQVariantListToObject(val.toVariant(QJSValue::ConvertJSObjects).toList());
                    } else if (val.isObject()) {
                        const auto var = val.toVariant(QJSValue::ConvertJSObjects);
                        const auto map = var.toMap();
                        const auto javaMap = JNIObject<JavaHashMap>::makeObject();
                        for (auto it = map.constBegin(); it != map.constEnd(); ++it) {
                            const auto jKey = JNIObject<JavaLangString>::makeObject(it.key());
                            const auto jVal = convertQVariantToObject(it.value());
                            JNIObject<JavaMap>::callMethod<jobject>(javaMap, "put", jKey, jVal);
                        }
                        return javaMap;
                    }
                }
                qCWarning(QT_BRIDGE) << "Unsupported QVariant type:" << var.typeName();
                return {};
        }
    }
    jobject Converter::convertQVariantListToObject(const QVariantList &list)
    {
        const auto arrayListObj = JNIObject<JavaArrayList>::makeObject();
        for (const QVariant &elem : list) {
            const auto jResult = Converter::convertQVariantToObject(elem);
            JNIObject<JavaList>::callMethod<jboolean>(arrayListObj,"add",jResult);
        }
        return arrayListObj;
    }

    jobject Converter::convertQVariantListToArray(JNIEnv *env, const QVariantList &list, qint8 elemType)
    {
        const bool isPrimitive = typeIsPrimitive(elemType);
        const VariableType type = varType(elemType);
        const jsize size = static_cast<jsize>(list.size());

        auto warn = [&list](int i, const char *expected) {
            qCWarning(QT_BRIDGE, "Invokable expects %s[]; element %d is %s",
                      expected, i, list.at(i).typeName());
        };

        switch (type) {
        case VariableType::Boolean:
            if (isPrimitive) {
                jbooleanArray array = env->NewBooleanArray(size);
                QList<jboolean> tmp;
                tmp.reserve(size);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<bool>())
                        warn(i, "boolean");
                    tmp.append(static_cast<jboolean>(value.toBool()));
                }
                env->SetBooleanArrayRegion(array, 0, size, tmp.constData());
                return array;
            } else {
                jclass clazz = JNIObject<JavaLangBoolean>::get();
                jobjectArray array = env->NewObjectArray(size, clazz, nullptr);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<bool>())
                        warn(i, "Boolean");
                    jobject object = JNIObject<JavaLangBoolean>::makeObject(static_cast<jboolean>(value.toBool()));
                    env->SetObjectArrayElement(array, i, object);
                    env->DeleteLocalRef(object);
                }
                return array;
            }
        case VariableType::Byte:
            if (isPrimitive) {
                jbyteArray array = env->NewByteArray(size);
                QList<jbyte> tmp;
                tmp.reserve(size);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<qint8>()) // jbyte ~ qint8 (signed char)
                        warn(i, "byte");
                    tmp.append(static_cast<jbyte>(value.value<qint8>()));
                }
                env->SetByteArrayRegion(array, 0, size, tmp.constData());
                return array;
            } else {
                jclass clazz = JNIObject<JavaLangByte>::get();
                jobjectArray array = env->NewObjectArray(size, clazz, nullptr);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<qint8>())
                        warn(i, "Byte");
                    jobject object = JNIObject<JavaLangByte>::makeObject(static_cast<jbyte>(value.value<qint8>()));
                    env->SetObjectArrayElement(array, i, object);
                    env->DeleteLocalRef(object);
                }
                return array;
            }
        case VariableType::Char: {
            auto toJChar = [&warn](const QVariant &value, int i, const char *expected) -> jchar {
                if (value.canConvert<QChar>())
                    return static_cast<jchar>(value.toChar().unicode());

                // QML/JS has no char type, and it may actually give a single character QString
                if (value.typeId() == QMetaType::QString) {
                    const QString s = value.toString();
                    if (s.size() == 1)
                        return static_cast<jchar>(s.at(0).unicode());
                }

                warn(i, expected);
                return static_cast<jchar>(value.toChar().unicode());
            };

            if (isPrimitive) {
                jcharArray array = env->NewCharArray(size);
                QList<jchar> tmp;
                tmp.reserve(size);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    tmp.append(toJChar(value, i, "char"));
                }
                env->SetCharArrayRegion(array, 0, size, tmp.constData());
                return array;
            } else {
                jclass clazz = JNIObject<JavaLangCharacter>::get();
                jobjectArray array = env->NewObjectArray(size, clazz, nullptr);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    jobject object = JNIObject<JavaLangCharacter>::makeObject(
                        toJChar(value, i, "Character"));
                    env->SetObjectArrayElement(array, i, object);
                    env->DeleteLocalRef(object);
                }
                return array;
            }
        }
        case VariableType::Short:
            if (isPrimitive) {
                jshortArray array = env->NewShortArray(size);
                QList<jshort> tmp;
                tmp.reserve(size);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<short>())
                        warn(i, "short");
                    tmp.append(static_cast<short>(value.value<short>()));
                }
                env->SetShortArrayRegion(array, 0, size, tmp.constData());
                return array;
            } else {
                jclass clazz = JNIObject<JavaLangShort>::get();
                jobjectArray array = env->NewObjectArray(size, clazz, nullptr);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<short>())
                        warn(i, "Short");
                    jobject object = JNIObject<JavaLangShort>::makeObject(static_cast<jshort>(value.value<short>()));
                    env->SetObjectArrayElement(array, i, object);
                    env->DeleteLocalRef(object);
                }
                return array;
            }
        case VariableType::Int:
            if (isPrimitive) {
                jintArray array = env->NewIntArray(size);
                QList<jint> tmp;
                tmp.reserve(size);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<int>())
                        warn(i, "int");
                    tmp.append(static_cast<jint>(value.toInt()));
                }
                env->SetIntArrayRegion(array, 0, size, tmp.constData());
                return array;
            } else {
                jclass clazz = JNIObject<JavaLangInteger>::get();
                jobjectArray array = env->NewObjectArray(size, clazz, nullptr);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<int>())
                        warn(i, "Integer");
                    jobject object = JNIObject<JavaLangInteger>::makeObject(static_cast<jint>(value.toInt()));
                    env->SetObjectArrayElement(array, i, object);
                    env->DeleteLocalRef(object);
                }
                return array;
            }
        case VariableType::Long:
            if (isPrimitive) {
                jlongArray array = env->NewLongArray(size);
                QList<jlong> tmp;
                tmp.reserve(size);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<qint64>()) // qint64 ~ jlong ~ long long
                        warn(i, "long");
                    tmp.append(static_cast<jlong>(value.toLongLong()));
                }
                env->SetLongArrayRegion(array, 0, size, tmp.constData());
                return array;
            } else {
                jclass clazz = JNIObject<JavaLangLong>::get();
                jobjectArray array = env->NewObjectArray(size, clazz, nullptr);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<qint64>())
                        warn(i, "Long");
                    jobject object = JNIObject<JavaLangLong>::makeObject(static_cast<jlong>(value.toLongLong()));
                    env->SetObjectArrayElement(array, i, object);
                    env->DeleteLocalRef(object);
                }
                return array;
            }
        case VariableType::Float:
            if (isPrimitive) {
                jfloatArray array = env->NewFloatArray(size);
                QList<jfloat> tmp;
                tmp.reserve(size);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<float>())
                        warn(i, "float");
                    tmp.append(static_cast<jfloat>(value.toFloat()));
                }
                env->SetFloatArrayRegion(array, 0, size, tmp.constData());
                return array;
            } else {
                jclass clazz = JNIObject<JavaLangFloat>::get();
                jobjectArray array = env->NewObjectArray(size, clazz, nullptr);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<float>())
                        warn(i, "Float");
                    jobject object = JNIObject<JavaLangFloat>::makeObject(static_cast<jfloat>(value.toFloat()));
                    env->SetObjectArrayElement(array, i, object);
                    env->DeleteLocalRef(object);
                }
                return array;
            }
        case VariableType::Double:
            if (isPrimitive) {
                jdoubleArray array = env->NewDoubleArray(size);
                QList<jdouble> tmp;
                tmp.reserve(size);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<double>())
                        warn(i, "double");
                    tmp.append(static_cast<jdouble>(value.toDouble()));
                }
                env->SetDoubleArrayRegion(array, 0, size, tmp.constData());
                return array;
            } else {
                jclass clazz = JNIObject<JavaLangDouble>::get();
                jobjectArray array = env->NewObjectArray(size, clazz, nullptr);
                for (int i = 0; i < list.size(); ++i) {
                    const QVariant &value = list.at(i);
                    if (!value.canConvert<double>())
                        warn(i, "Double");
                    jobject object = JNIObject<JavaLangDouble>::makeObject(static_cast<jdouble>(value.toDouble()));
                    env->SetObjectArrayElement(array, i, object);
                    env->DeleteLocalRef(object);
                }
                return array;
            }
        case VariableType::String: {
            jclass clazz = JNIObject<JavaLangString>::get();
            jobjectArray array = env->NewObjectArray(size, clazz, nullptr);
            for (int i = 0; i < list.size(); ++i) {
                const QVariant &value = list.at(i);
                jobject string = JNIObject<JavaLangString>::makeObject(value.toString());
                env->SetObjectArrayElement(array, i, string);
                env->DeleteLocalRef(string);
            }
            return array;
        }
        case VariableType::QmlRegistrable: {
            const jclass clazz = JNIObject<JavaLangObject>::get();
            jobjectArray array = env->NewObjectArray(size, clazz, nullptr);
            if (!array)
                return nullptr;
            for (int i = 0; i < list.size(); ++i) {
                const QVariant &value = list.at(i);
                jobject object = Converter::convertQVariantToObject(value);
                env->SetObjectArrayElement(array, i, object);
                if (object)
                    env->DeleteLocalRef(object);
            }
            return array;
        }
        default:
            qCWarning(QT_BRIDGE, "Unsupported invokable array parameter type: %d", int(type));
        }
        return nullptr;
    }

    jobject Converter::convertQVariantMapToObject(const QVariantMap &map)
    {
        const auto env = JniContext::getEnv();
        const jobject jMap = JNIObject<JavaHashMap>::makeObject(); // new java.util.HashMap()

        // Populate the Java HashMap (recursively if needed)
        for (auto it = map.constBegin(); it != map.constEnd(); ++it) {
            // Keys in QVariantMap are QString
            jobject jKey = JNIObject<JavaLangString>::makeObject(it.key());

            // Convert value
            const QVariant &v = it.value();
            jobject jVal = nullptr;
            switch (v.typeId()) {
            case QMetaType::QVariantMap:
                jVal = convertQVariantMapToObject(v.toMap());
                break;
            case QMetaType::QVariantList:
                jVal = convertQVariantListToObject(v.toList());
                break;
            case QMetaType::QStringList:
                jVal = convertQStringListToObject(v.toStringList());
                break;
            default:
                // Primitives: QString, int, double, etc.
                jVal = Converter::convertQVariantToObject(v);
                break;
            }

            // Put the (key, value) in the map
            JNIObject<JavaMap>::callMethod<jobject>(jMap, "put", jKey, jVal);
            checkAndClearException(env);

            // Release local handles created in this loop iteration
            if (jKey) env->DeleteLocalRef(jKey);
            if (jVal) env->DeleteLocalRef(jVal);
        }

        return jMap;
    }
    jobject Converter::convertQStringListToObject(const QStringList &list)
    {
        const auto arrayListObj = JNIObject<JavaArrayList>::makeObject();
        for (const QString &elem : list) {
            const auto jResult = JNIObject<JavaLangString>::makeObject(elem);
            JNIObject<JavaList>::callMethod<jboolean>(arrayListObj,"add",jResult);
        }
        return arrayListObj;
    }

    jobject Converter::convertQStringListToArray(JNIEnv *env, const QStringList &list)
    {
        const jsize size = static_cast<jsize>(list.size());
        jclass clazz = JNIObject<JavaLangString>::get();
        jobjectArray array = env->NewObjectArray(size, clazz, nullptr);
        for (int i = 0; i < list.size(); ++i) {
            jobject string = JNIObject<JavaLangString>::makeObject(list.at(i));
            env->SetObjectArrayElement(array, i, string);
            env->DeleteLocalRef(string);
        }
        return array;
    }


} // namespace Utility::JNI
