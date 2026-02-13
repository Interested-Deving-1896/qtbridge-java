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

    // Checks if the upmost bit is set, which indicates the type is primitive
    inline bool typeIsPrimitive(qint8 packed) {
        return (static_cast<quint8>(packed) & 0x80u) != 0u;
    }

    // Returns VarType (removes potential primitive flagging)
    inline VarType type(qint8 packed) {
        return static_cast<VarType>(static_cast<quint8>(packed) & 0x7Fu);
    }

    inline VarShape paramShape(const JNICache::JMethodEntry &e, int idx) {
        Q_ASSERT(idx >= 0 && idx < e.parmShape.size());
        return static_cast<VarShape>(e.parmShape.at(idx));
    }

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
            ret.l = convertQVariantListToObject(qlist);
            break;
        }
        case QMetaType::QStringList: {
            QStringList &slist = *static_cast<QStringList *>(cppParameter);
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
            double cppValue = *static_cast<long long*>(cppParameter);
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

    // Used to convert signal parameters. Signal parameters are always boxed because
    // they go through the signal proxy which auto-boxes them.
    bool Converter::javaParameterToCppParameter(int cppMetaTypeId, JNIEnv *env, jobject valueObj, void *outPtr)
    {
        Q_ASSERT(outPtr);

        if (!valueObj) {
            // Leave the default-constructed value produced by QMetaType::create()
            // in the caller. This makes Java 'null' map to a sensible C++ default
            // (e.g. empty QString, empty QVariantMap, nullptr for QObject*, etc.).
            return true;
        }

        switch (cppMetaTypeId) {
        case QMetaType::QVariant: {
            *static_cast<QVariant *>(outPtr) = convertObjectToQVariant(valueObj);
            return true;
        }
        case QMetaType::QVariantList: {
            *static_cast<QVariantList *>(outPtr) = convertJavaListToQVariantList(valueObj);
            return true;
        }
        case QMetaType::QStringList: {
            *static_cast<QStringList *>(outPtr) = convertJavaToQStringList(valueObj);
            return true;
        }
        case QMetaType::LongLong: {
            const auto value = JNIObject<JavaLangLong>::callMethod<jlong>(valueObj, "longValue");
            *static_cast<long long *>(outPtr) = value;
            return true;
        }
        case QMetaType::QString: {
            *static_cast<QString *>(outPtr) = Utility::JNI::toQString(jstring(valueObj));
            return true;
        }
        case QMetaType::Float: {
            const auto value = JNIObject<JavaLangFloat>::callMethod<jfloat>(valueObj, "floatValue");
            *static_cast<float *>(outPtr) = value;
            return true;
        }
        case QMetaType::Bool: {
            const auto value = JNIObject<JavaLangBoolean>::callMethod<jboolean>(valueObj, "booleanValue");
            *static_cast<bool *>(outPtr) = value;
            return true;
        }
        case QMetaType::QChar: {
            const auto value = JNIObject<JavaLangCharacter>::callMethod<jchar>(valueObj, "charValue");
            QChar qc(value);
            *static_cast<QChar *>(outPtr) = qc;
            return true;
        }
        case QMetaType::SChar: {
            // QMetaType::SChar == signed char == qint8 == jbyte
            const auto value = JNIObject<JavaLangByte>::callMethod<jbyte>(valueObj, "byteValue");
            *static_cast<signed char *>(outPtr) = value;
            return true;
        }
        case QMetaType::Short: {
            const auto value = JNIObject<JavaLangShort>::callMethod<jshort>(valueObj, "shortValue");
            *static_cast<short *>(outPtr) = value;
            return true;
        }
        case QMetaType::Int: {
            const auto value = JNIObject<JavaLangInteger>::callMethod<jint>(valueObj, "intValue");
            *static_cast<int *>(outPtr) = value;
            return true;
        }
        case QMetaType::Double: {
            const auto value = JNIObject<JavaLangDouble>::callMethod<jdouble>(valueObj, "doubleValue");
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
            if (JNIObject<JavaMap>::isInstanceOf(valueObj)) {
                // Map<String, Object> -> QVariantMap
                map = convertJavaMapToQVariantMap(valueObj);
            } else if (JNIObject<JavaLangEnum>::isInstanceOf(valueObj)) {
                // Enum -> QVariantMap
                map = convertEnumToQVariantMap(valueObj);
            } else {
                qCWarning(QT_BRIDGE, "convertJavaToMetaType: expected Map or Enum "
                                     "for QVariantMap param");
                *static_cast<QVariantMap *>(outPtr) = QVariantMap{};
                return false;
            }
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
        if (javaObject == nullptr) {
            // If the object is null, set a proper Javascript null value (instead of undefined)
            return QVariant::fromValue(QJSValue(QJSValue::NullValue));
        }
        if (JNIObject<JavaLangInteger>::isInstanceOf(javaObject))
            return int(JNIObject<JavaLangInteger>::callMethod<jint>(javaObject, "intValue"));
        if (JNIObject<JavaLangBoolean>::isInstanceOf(javaObject))
            return bool(JNIObject<JavaLangBoolean>::callMethod<jboolean>(javaObject, "booleanValue"));
        if (JNIObject<JavaLangDouble>::isInstanceOf(javaObject))
            return double(JNIObject<JavaLangDouble>::callMethod<jdouble>(javaObject, "doubleValue"));
        if (JNIObject<JavaLangFloat>::isInstanceOf(javaObject))
            return float(JNIObject<JavaLangFloat>::callMethod<jfloat>(javaObject, "floatValue"));
        if (JNIObject<JavaLangLong>::isInstanceOf(javaObject))
            return qint64(JNIObject<JavaLangLong>::callMethod<jlong>(javaObject, "longValue"));
        if (JNIObject<JavaLangByte>::isInstanceOf(javaObject))
            return qint8(JNIObject<JavaLangByte>::callMethod<jbyte>(javaObject, "byteValue"));
        if (JNIObject<JavaLangShort>::isInstanceOf(javaObject))
           return short(JNIObject<JavaLangShort>::callMethod<jshort>(javaObject, "shortValue"));
        if (JNIObject<JavaLangCharacter>::isInstanceOf(javaObject))
           return QChar(JNIObject<JavaLangCharacter>::callMethod<jchar>(javaObject, "charValue"));

        if (JNIObject<JavaLangString>::isInstanceOf(javaObject))
            return Utility::JNI::toQString(jstring(javaObject));

        if (JNIObject<JavaLangEnum>::isInstanceOf(javaObject))
            return convertEnumToQVariantMap(javaObject);

        if (JNIObject<JavaList>::isInstanceOf(javaObject))
           return convertJavaListToQVariantList(javaObject);

        if (JNIObject<JavaMap>::isInstanceOf(javaObject))
            return convertJavaMapToQVariantMap(javaObject);

        if (JNIObject<JavaNetURI>::isInstanceOf(javaObject)) {
            const auto s = JNIObject<JavaNetURI>::callMethod<QString>(javaObject, "toString");
            return QUrl{s};
        }
        // Keep Object check last, as it's the most generic one
        if (JNIObject<JavaLangObject>::isInstanceOf(javaObject)) {
            auto env = JniContext::getEnv();
            QObject *proxy = JNIProxyUserObjectMap::ensureProxy(env, javaObject, false);
            return QVariant::fromValue(proxy);
        }

        qCWarning(QT_BRIDGE, "Unsupported Java type for QVariant conversion");
        return {};
    }

    QVariantMap Converter::convertJavaMapToQVariantMap(const jobject &jmap)
    {
        const auto mapSize = JNIObject<JavaMap>::callMethod<jint>(jmap, "size");
        if (mapSize == 0)
            return {};
        const auto entrySet = JNIObject<JavaMap>::callMethod<JavaSet>(jmap, "entrySet");
        const auto iterator = JNIObject<JavaSet>::callMethod<JavaIterator>(entrySet, "iterator");
        QVariantMap qtMap;
        for (jint i = 0; i < mapSize; ++i) {
            const auto entry = JNIObject<JavaIterator>::callMethod<jobject>(iterator, "next");
            const auto key = JNIObject<JavaMapEntry>::callMethod<jobject>(entry, "getKey");
            const auto value = JNIObject<JavaMapEntry>::callMethod<jobject>(entry, "getValue");
            const auto qtKey = convertObjectToQVariant(key).toString();
            const auto qtValue = convertObjectToQVariant(value);
            qtMap.insert(qtKey, qtValue);
        }
        return qtMap;
    }

    QVariantList Converter::convertJavaListToQVariantList(const jobject &javaObject)
    {
        const auto size = JNIObject<JavaList>::callMethod<jint>(javaObject, "size");
        QVariantList qtList;
        for (jint i = 0; i < size; ++i) {
            const auto element = JNIObject<JavaList>::callMethod<jobject>(javaObject, "get", i);
            qtList.append(convertObjectToQVariant(element));
        }
        return qtList;
    }
    QStringList Converter::convertJavaToQStringList(const jobject &javaObject)
    {
        const auto size = JNIObject<JavaList>::callMethod<jint>(javaObject, "size");
        QStringList qtList;
        for (jint i = 0; i < size; ++i) {
            const auto element = JNIObject<JavaList>::callMethod<jobject>(javaObject, "get", i);
            qtList.append(toQString(static_cast<jstring>(element)));
        }
        return qtList;
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
} // namespace Utility::JNI
