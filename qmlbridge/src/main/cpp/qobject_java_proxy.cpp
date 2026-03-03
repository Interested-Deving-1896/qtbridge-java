/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#include "qobject_java_proxy.h"

#include "jni_cache.h"
#include "jni_context.h"
#include "jni_method_invoker.h"
#include "jni_object.h"
#include "jni_proxy_userobject_map.h"
#include "jni_type.h"

#include <QtCore/qloggingcategory.h>
#include <QtCore/qabstractitemmodel.h>
#include <QtCore/qurl.h>

using namespace Utility::JNI;

Q_LOGGING_CATEGORY(QT_BRIDGE, "qtproject.qt.bridge")

QObjectJavaProxy::QObjectJavaProxy(qint64 cacheKey, QObject *parent)
    : QtProxyBase(parent), m_cacheKey(cacheKey)
{
    m_dynamicMetaObject = std::make_unique<JniDynamicMetaObject>("QObjectJavaProxy", &staticMetaObject);
}

QObjectJavaProxy::~QObjectJavaProxy()
{
    // Destruction during shutdown may happen after JVM is gone,
    // in particular with the context creation prototype items.
    // Return early to avoid unnecessary JVM attach failure warning
    if (!m_userObject && !m_qtObject)
        return;

    auto *env = JniContext::getEnv();
    if (!env)
        return;

    if (m_userObject) {
        JNIProxyUserObjectMap::removeByProxy(this);

        if (m_ownedByQml)
            env->DeleteGlobalRef(m_userObject);
        else
            env->DeleteWeakGlobalRef(m_userObject);

        m_userObject = nullptr;
    }

    if (m_qtObject) {
        env->DeleteGlobalRef(m_qtObject);
        m_qtObject = nullptr;
    }
}

void QObjectJavaProxy::classBegin()
{}

void QObjectJavaProxy::componentComplete()
{
    const auto completionHandlerEntry = JNICache::qmlCompletionHandler(m_cacheKey);
    if (!completionHandlerEntry)
        return;

    const auto localRef = userObjectLocalRef();
    if (!localRef) {
        qCWarning(QT_BRIDGE, "QML completion handler target object is missing");
        return;
    }
    JNIEnv *env = JniContext::getEnv();
    env->CallVoidMethod(localRef, completionHandlerEntry->method);
    checkAndClearException(env);
    env->DeleteLocalRef(localRef);
}

void QObjectJavaProxy::registerUserObject(jobject userObjectLocalRef, bool ownedByQml)
{
    Q_ASSERT(userObjectLocalRef);
    JNIEnv* env = JniContext::getEnv();

    // Drop previous ref using the previous ownership
    if (m_userObject) {
        if (m_ownedByQml)
            env->DeleteGlobalRef(m_userObject);
        else
            env->DeleteWeakGlobalRef(m_userObject);
        m_userObject = nullptr;
    }

    m_ownedByQml = ownedByQml;

    // Create the correct kind of ref for the new owner
    if (m_ownedByQml)
        m_userObject = env->NewGlobalRef(userObjectLocalRef);
    else
        m_userObject = env->NewWeakGlobalRef(userObjectLocalRef);

    JNIProxyUserObjectMap::addMapping(userObjectLocalRef, this);
}

void QObjectJavaProxy::registerQtObject(jobject qtObject)
{
    if (m_qtObject) {
        JniContext::getEnv()->DeleteGlobalRef(m_qtObject);
    }
    m_qtObject = JniContext::getEnv()->NewGlobalRef(qtObject);
}

jobject QObjectJavaProxy::userObjectLocalRef()
{
    return m_userObject
               ? JniContext::getEnv()->NewLocalRef(m_userObject)
               : nullptr;
}

void QObjectJavaProxy::qtReadPropertyMetacall(const jobject javaObject,
                                              const int propertyIndex, void **args)
{
    const auto mp = metaObject()->property(propertyIndex);
    if (!mp.isReadable()) {
        qCDebug(QT_BRIDGE, "Property is not readable: %s",  mp.name());
        return;
    }
    if (mp.userType() == qMetaTypeId<QAbstractItemModel*>()) {
        readItemModelProperty(javaObject, mp, args);
        return;
    }

    const auto &entry = JNICache::getProxyField(cacheKey(), mp.propertyIndex());
    if (!entry.field) {
        qCWarning(QT_BRIDGE, "Property read: could not find entry for %s %s",
                  mp.name(), mp.typeName());
        setPropertyDefaultValue(args, mp.metaType());
        return;
    }

    jobject holderLocal = nullptr; // the field (QtProperty<T>)
    jobject valueLocal  = nullptr; // the actual value (boxed T, String, Map, Enum, etc.)
    JNIEnv *env = JniContext::getEnv();

    auto guard = qScopeGuard([&]{
        if (valueLocal && valueLocal != holderLocal)
            env->DeleteLocalRef(valueLocal);
        if (holderLocal)
            env->DeleteLocalRef(holderLocal);
    });

    holderLocal = env->GetObjectField(javaObject, entry.field);
    if (!holderLocal) {
        qCWarning(QT_BRIDGE, "Property read failed, field %s is null", mp.name());
        setPropertyDefaultValue(args, mp.metaType());
        return;
    }

    // QtProperty<T> – getValue() returns boxed T (or null)
    valueLocal = JNIMethodInvoker::invokeMethod<jobject>(
        env, holderLocal, JNICache::qtPropertyGetValueMethod());
    if (!valueLocal) {
        setPropertyDefaultValue(args, mp.metaType());
        return;
    }

    const auto metaId = mp.metaType().id();

    switch (metaId) {
    case QMetaType::Int:
        *static_cast<int *>(args[0]) = JNIObject<JavaLangInteger>::callMethod<jint>(valueLocal, "intValue");
        break;
    case QMetaType::SChar:
        // QMetaType::SChar == signed char == qint8 == jbyte
        *static_cast<signed char *>(args[0]) = JNIObject<JavaLangByte>::callMethod<jbyte>(valueLocal, "byteValue");
        break;
    case QMetaType::QChar:
        // Java Character and QChar are both UTF-16 characters
        *static_cast<QChar *>(args[0]) = JNIObject<JavaLangCharacter>::callMethod<jchar>(valueLocal, "charValue");
        break;
    case QMetaType::Bool:
        *static_cast<bool *>(args[0]) = JNIObject<JavaLangBoolean>::callMethod<jboolean>(valueLocal, "booleanValue");
        break;
    case QMetaType::Float:
        *static_cast<float *>(args[0]) = JNIObject<JavaLangFloat>::callMethod<jfloat>(valueLocal, "floatValue");
        break;
    case QMetaType::Double:
        *static_cast<double *>(args[0]) = JNIObject<JavaLangDouble>::callMethod<jdouble>(valueLocal, "doubleValue");
        break;
    case QMetaType::LongLong:
        *static_cast<long long *>(args[0]) = JNIObject<JavaLangLong>::callMethod<jlong>(valueLocal, "longValue");
        break;
    case QMetaType::Short:
        *static_cast<short *>(args[0]) = JNIObject<JavaLangShort>::callMethod<jshort>(valueLocal, "shortValue");
        break;
    case QMetaType::QString:
        *static_cast<QString *>(args[0]) = Utility::JNI::toQString(jstring(valueLocal));
        break;
    case QMetaType::QStringList:
        if (static_cast<VariableShape>(entry.shape) == VariableShape::Array)
            *static_cast<QStringList *>(args[0]) = Converter::convertJavaArrayToQStringList(env, valueLocal);
        else
            *static_cast<QStringList *>(args[0]) = Converter::convertJavaListToQStringList(valueLocal);
        break;
    case QMetaType::QVariant:
        *static_cast<QVariant *>(args[0]) = Converter::convertObjectToQVariant(valueLocal);
        break;
    case QMetaType::QVariantList:
        if (static_cast<VariableShape>(entry.shape) == VariableShape::Array)
            *static_cast<QVariantList *>(args[0]) = Converter::convertJavaArrayToQVariantList(env, valueLocal, entry.type);
        else
            *static_cast<QVariantList *>(args[0]) = Converter::convertJavaListToQVariantList(valueLocal);
        break;
    case QMetaType::QVariantMap:
        if (JNIObject<JavaMap>::isInstanceOf(valueLocal))
            *static_cast<QVariantMap *>(args[0]) = Converter::convertJavaMapToQVariantMap(valueLocal);
        if (JNIObject<JavaLangEnum>::isInstanceOf(valueLocal))
            *static_cast<QVariantMap *>(args[0]) = Converter::convertEnumToQVariantMap(valueLocal);
        break;
    case QMetaType::QUrl: {
        const auto value = JNIObject<JavaNetURI>::callMethod<jstring>(valueLocal, "toString");
        *static_cast<QUrl *>(args[0]) = QUrl(Utility::JNI::toQString(value));
        break;
    }
    case QMetaType::QObjectStar:
        readQmlRegistrableProperty(javaObject, mp, args);
        break;
    default:
        qCWarning(QT_BRIDGE, "Property read: unsupported type %s %s", mp.name(), mp.typeName());
        break;
    }
}
// QML writes to a property of the proxy object.
// Converts arg[0] to a Java-object and calls QtProperty::setValue() with it.
// Here we can assume values to be boxed because primitives won't compile with
// Java generics (QtProperty<T>).
void QObjectJavaProxy::qtWritePropertyMetacall(const jobject javaObject,
                                               const int propertyIndex, void **args)
{
    const auto mp = metaObject()->property(propertyIndex);
    if (!mp.isWritable()) {
        qCWarning(QT_BRIDGE, "Property is not writable: %s", mp.name());
        return;
    }

    const auto &entry = JNICache::getProxyField(cacheKey(), mp.propertyIndex());
    if (!entry.field) {
        qCWarning(QT_BRIDGE, "Property write: unable to find entry for: %s", mp.name());
        return;
    }

    const auto metaType = mp.metaType();
    JNIEnv *env = JniContext::getEnv();
    jobject valueObj = nullptr;

    switch (metaType.id()) {
    case QMetaType::Int:
        valueObj = JNIObject<JavaLangInteger>::makeObject(*static_cast<jint *>(args[0]));
        break;
    case QMetaType::SChar:
        valueObj = JNIObject<JavaLangByte>::makeObject(*static_cast<jbyte *>(args[0]));
        break;
    case QMetaType::QChar:
        valueObj = JNIObject<JavaLangCharacter>::makeObject(*static_cast<jchar *>(args[0]));
        break;
    case QMetaType::Bool:
        valueObj = JNIObject<JavaLangBoolean>::makeObject(*static_cast<jboolean *>(args[0]));
        break;
    case QMetaType::Float:
        valueObj = JNIObject<JavaLangFloat>::makeObject(*static_cast<jfloat *>(args[0]));
        break;
    case QMetaType::Double:
        valueObj = JNIObject<JavaLangDouble>::makeObject(*static_cast<jdouble *>(args[0]));
        break;
    case QMetaType::LongLong:
        valueObj = JNIObject<JavaLangLong>::makeObject(*static_cast<jlong *>(args[0]));
        break;
    case QMetaType::Short:
        valueObj = JNIObject<JavaLangShort>::makeObject(*static_cast<jshort *>(args[0]));
        break;
    case QMetaType::QVariant:
        valueObj = Converter::convertQVariantToObject(*static_cast<QVariant *>(args[0]));
        break;
    case QMetaType::QVariantList:
        if (static_cast<VariableShape>(entry.shape) == VariableShape::Array)
            valueObj = Converter::convertQVariantListToArray(env, *static_cast<QVariantList *>(args[0]), entry.type);
        else
            valueObj = Converter::convertQVariantListToObject(*static_cast<QVariantList *>(args[0]));
        break;
    case QMetaType::QStringList:
        if (static_cast<VariableShape>(entry.shape) == VariableShape::Array)
            valueObj = Converter::convertQStringListToArray(env, *static_cast<QStringList *>(args[0]));
        else
            valueObj = Converter::convertQStringListToObject(*static_cast<QStringList *>(args[0]));
        break;
    case QMetaType::QVariantMap:
        valueObj = Converter::convertQVariantMapToObject(*static_cast<QVariantMap *>(args[0]));
        break;
    case QMetaType::QString: {
        const QString qstr = *static_cast<QString *>(args[0]);
        valueObj = env->NewString(reinterpret_cast<const jchar*>(qstr.utf16()), qstr.size());
        break;
    }
    case QMetaType::QUrl: {
        const QUrl url = *static_cast<QUrl *>(args[0]);
        const QString urlString = url.toString(QUrl::FullyEncoded);
        jstring value = env->NewStringUTF(urlString.toUtf8().constData());
        valueObj = JNIObject<JavaNetURI>::makeObject(value);
        checkAndClearException(env);
        break;
    }
    case QMetaType::QObjectStar:
        writeQmlRegistrableProperty(javaObject, mp, args);
        return;
    default:
        qCWarning(QT_BRIDGE, "Property write: unsupported type %s %s", mp.name(), mp.typeName());
        return;
    };

    if (!valueObj) {
        qCWarning(QT_BRIDGE, "Property write failed, value object creation failed for %s", mp.name());
        return;
    }
    // QtProperty<T> – call setValue(Object) on the QtProperty instance
    jobject fieldObject = env->GetObjectField(javaObject, entry.field);

    JNIMethodInvoker::invokeMethod<void>(
        env, fieldObject, JNICache::qtPropertySetValueMethod(), valueObj);

    env->DeleteLocalRef(fieldObject);
}

void QObjectJavaProxy::qtMethodMetacall(const jobject javaObject, const int methodIndex,
                                       void **args)
{
    const auto method = metaObject()->method(methodIndex);

    if (method.methodType() == QMetaMethod::Signal) {
        QMetaObject::activate(this, methodIndex, args);
        return;
    }

    const auto methodCacheEntry = JNICache::getProxyMethod(cacheKey(), methodIndex);
    if (!methodCacheEntry) {
        qCWarning(QT_BRIDGE, "Invokable method %s not found", method.name().constData());
        return;
    }
    // If upmost bit is set, value Java-side representation is primitive (int instead of Integer)
    const bool retIsPrimitive = (static_cast<quint8>(methodCacheEntry->retType) & 0x80u) != 0u;
    const auto retShape = static_cast<VariableShape>(methodCacheEntry->retShape);

    // First convert and collect the function parameters into a list.
    const auto parameterCount = method.parameterCount();
    QList<jvalue> parameters;
    parameters.reserve(parameterCount);
    JNIEnv* env = JniContext::getEnv();
    // Convert invokable's parameters from cpp types to java types
    for (int parameterIndex = 0; parameterIndex < parameterCount; ++parameterIndex) {
        void *arg = args[parameterIndex + 1];
        const jvalue value = Converter::cppParameterToJavaParameter(
            env, method, arg, *methodCacheEntry, parameterIndex);
        parameters.push_back(value);
    }

    // Call the invokable method, switch based on the return-type
    switch (method.returnType()) {
    case QMetaType::QVariant: {
        const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
            env, javaObject, methodCacheEntry->method, parameters.data());
        const auto value = Converter::convertObjectToQVariant(ret);
        *static_cast<QVariant *>(args[0]) = value;
        break;
    }
    case QMetaType::QVariantMap: {
        const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
            env, javaObject, methodCacheEntry->method, parameters.data());
        if (JNIObject<JavaLangEnum>::isInstanceOf(ret))
            *static_cast<QVariantMap *>(args[0]) = Converter::convertEnumToQVariantMap(ret);
        else
            *static_cast<QVariantMap *>(args[0]) = Converter::convertJavaMapToQVariantMap(ret);
        break;
    }
    case QMetaType::QVariantList: {
        const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
            env, javaObject, methodCacheEntry->method, parameters.data());
        QVariantList value;
        if (retShape == VariableShape::Array)
            value = Converter::convertJavaArrayToQVariantList(env, ret, methodCacheEntry->retType);
        else
            value = Converter::convertJavaListToQVariantList(ret);
        *static_cast<QVariantList *>(args[0]) = value;
        break;
    }
    case QMetaType::QStringList: {
        const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
            env, javaObject, methodCacheEntry->method, parameters.data());
        QStringList value;
        if (retShape == VariableShape::Array)
            value = Converter::convertJavaArrayToQStringList(env, ret);
        else
            value = Converter::convertJavaListToQStringList(ret);
        *static_cast<QStringList *>(args[0]) = value;
        break;
    }
    case QMetaType::QUrl: {
        const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
            env, javaObject, methodCacheEntry->method, parameters.data());
        const QUrl value(JNIObject<JavaNetURI>::callMethod<QString>(ret, "toString"));
        *static_cast<QUrl *>(args[0]) = value;
        break;
    }
    case QMetaType::QString: {
        const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jstring>(
            env, javaObject, methodCacheEntry->method, parameters.data());
        *static_cast<QString *>(args[0]) = Utility::JNI::toQString(ret);
        break;
    }
    case QMetaType::LongLong:
        if (retIsPrimitive) {
            const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jlong>(
                env, javaObject, methodCacheEntry->method, parameters.data());
            *static_cast<long long *>(args[0]) = ret;
        } else {
            const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
                env, javaObject, methodCacheEntry->method, parameters.data());
            *static_cast<long long *>(args[0]) = JNIObject<JavaLangLong>::callMethod<jlong>(ret, "longValue");
        }
        break;
    case QMetaType::Double:
        if (retIsPrimitive) {
            *static_cast<double *>(args[0]) = JNIMethodInvoker::invokeMethodWithJValues<jdouble>(
                env, javaObject, methodCacheEntry->method, parameters.data());
        } else {
            const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
                env, javaObject, methodCacheEntry->method, parameters.data());
            *static_cast<double *>(args[0]) = JNIObject<JavaLangDouble>::callMethod<jdouble>(ret, "doubleValue");
        }
        break;
    case QMetaType::Float:
        if (retIsPrimitive) {
            *static_cast<float *>(args[0]) = JNIMethodInvoker::invokeMethodWithJValues<jfloat>(
                env, javaObject, methodCacheEntry->method, parameters.data());
        } else {
            const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
                env, javaObject, methodCacheEntry->method, parameters.data());
            *static_cast<float *>(args[0]) = JNIObject<JavaLangFloat>::callMethod<jfloat>(ret, "floatValue");
        }
        break;
    case QMetaType::Void:
        JNIMethodInvoker::invokeMethodWithJValues<void>(
            env, javaObject, methodCacheEntry->method, parameters.data());
        break;
    case QMetaType::Bool:
        if (retIsPrimitive) {
            *static_cast<bool *>(args[0]) = JNIMethodInvoker::invokeMethodWithJValues<jboolean>(
                env, javaObject, methodCacheEntry->method, parameters.data());
        } else {
            const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
                env, javaObject, methodCacheEntry->method, parameters.data());
            *static_cast<bool *>(args[0]) = JNIObject<JavaLangBoolean>::callMethod<jboolean>(ret, "booleanValue");
        }
        break;
    case QMetaType::QChar:
        jchar jc;
        if (retIsPrimitive) {
            jc = JNIMethodInvoker::invokeMethodWithJValues<jchar>(
                env, javaObject, methodCacheEntry->method, parameters.data());
        } else {
            const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
                env, javaObject, methodCacheEntry->method, parameters.data());
            jc = JNIObject<JavaLangCharacter>::callMethod<jchar>(ret, "charValue");
        }
        *static_cast<QChar *>(args[0]) = QChar(jc);
        break;
    case QMetaType::SChar:
        if (retIsPrimitive) {
            *static_cast<signed char *>(args[0]) = JNIMethodInvoker::invokeMethodWithJValues<jbyte>(
                env, javaObject, methodCacheEntry->method, parameters.data());
        } else {
            const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
                env, javaObject, methodCacheEntry->method, parameters.data());
            *static_cast<signed char *>(args[0]) = JNIObject<JavaLangByte>::callMethod<jbyte>(ret, "byteValue");
        }
        break;
    case QMetaType::Short:
        if (retIsPrimitive) {
            *static_cast<short *>(args[0]) = JNIMethodInvoker::invokeMethodWithJValues<jshort>(
                env, javaObject, methodCacheEntry->method, parameters.data());
        } else {
            const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
                env, javaObject, methodCacheEntry->method, parameters.data());
            *static_cast<short *>(args[0]) = JNIObject<JavaLangShort>::callMethod<jshort>(ret, "shortValue");
        }
        break;
    case QMetaType::Int:
        if (retIsPrimitive) {
            *static_cast<int *>(args[0]) = JNIMethodInvoker::invokeMethodWithJValues<jint>(
                env, javaObject, methodCacheEntry->method, parameters.data());
        } else {
            const auto ret = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
                env, javaObject, methodCacheEntry->method, parameters.data());
            *static_cast<int *>(args[0]) =
                JNIObject<JavaLangInteger>::callMethod<jint>(ret, "intValue");
        }
        break;
    case QMetaType::QObjectStar: {
        auto env = JniContext::getEnv();
        const auto userObject = JNIMethodInvoker::invokeMethodWithJValues<jobject>(
            env, javaObject, methodCacheEntry->method, parameters.data());
        // Ensure proxy object for the returned userObject. Since this is a function
        // return value, the potentially newly-created Java-object is owned by QML
        const auto proxy = JNIProxyUserObjectMap::ensureProxy(env, userObject, true /* ownedByQML */);
        *static_cast<QObject **>(args[0]) = proxy;
        break;
    }
    default:
        qCWarning(QT_BRIDGE, "Unsupported return type %s for invokable: %s ",
                 method.returnMetaType().name(), method.name().constData());
    }
}

void QObjectJavaProxy::readItemModelProperty(const jobject javaObject,
                                             const QMetaProperty &mp, void **args)
{
    const auto env = JniContext::getEnv();
    const auto& entry = JNICache::getProxyField(cacheKey(), mp.propertyIndex());
    *static_cast<QAbstractItemModel **>(args[0]) = nullptr;

    if (!entry.field) {
        qCWarning(QT_BRIDGE) << "No cached field entry for property" << mp.name();
        return;
    }

    const auto fieldObject = env->GetObjectField(javaObject, entry.field);
    if (!fieldObject) {
        qCWarning(QT_BRIDGE) << "Field" << mp.name() << "is null";
        return;
    }

    auto *nativeQObject = qobject_cast<QAbstractItemModel*>(
        reinterpret_cast<QObject*>(JNIUtilities::getNativeHandleFromObject(fieldObject)));

    env->DeleteLocalRef(fieldObject);
    if (!nativeQObject) {
        qCWarning(QT_BRIDGE) << "Failed to map Java object to QObject for property:" << mp.name();
        return;
    }
    *static_cast<QAbstractItemModel **>(args[0]) = nativeQObject;
}

void QObjectJavaProxy::readQmlRegistrableProperty(const jobject javaObject,
                                                  const QMetaProperty &mp, void **args)
{
    JNIEnv *env = JniContext::getEnv();
    *static_cast<QObject **>(args[0]) = nullptr; // return value

    const auto &entry = JNICache::getProxyField(cacheKey(), mp.propertyIndex());
    if (!entry.field) {
        qCWarning(QT_BRIDGE, "Unable to find entry for property %s", mp.name());
        return;
    }

    // Get holder object (QtProperty<T>)
    jobject holderLocal = env->GetObjectField(javaObject, entry.field);
    if (!holderLocal) {
        qCWarning(QT_BRIDGE, "Unable to find holding field for property %s", mp.name());
        return;
    }

    jobject userLocal = nullptr;
    auto guard = qScopeGuard([&](){
        if (userLocal && userLocal != holderLocal)
            env->DeleteLocalRef(userLocal);
        if (holderLocal)
            env->DeleteLocalRef(holderLocal);
    });

    // Extract actual user object
    userLocal = JNIMethodInvoker::invokeMethod<jobject>(
        env, holderLocal, JNICache::qtPropertyGetValueMethod());
    if (!userLocal)
        return; // Valid use-case: null userObject => return null proxy

    // Ensure we have a proxy, create a new one if we didn't.
    // Creating a new one means the user object is a new object set by
    // the user, and it doesn't yet have a proxy and QtObject. Lazily
    // build proxy + QtObject now. Since this is a return value from a
    // property read, Java owns the user object.
    *static_cast<QObject **>(args[0]) =
        JNIProxyUserObjectMap::ensureProxy(env, userLocal, false);
}

void QObjectJavaProxy::writeQmlRegistrableProperty(const jobject javaObject,
                                                   const QMetaProperty &mp, void **args)
{
    JNIEnv *env = JniContext::getEnv();

    // QML sets the proxy object, as it's not aware of the user object.
    // We need extract the userObject from that proxy object, and set it
    // on the Java-side.
    QObjectJavaProxy *proxy = *reinterpret_cast<QObjectJavaProxy**>(args[0]);
    jobject userObject = nullptr;
    // Proxy is null if a null-value is assigned at QML side
    if (proxy)
        userObject = proxy->userObjectLocalRef();

    const auto &entry = JNICache::getProxyField(cacheKey(), mp.propertyIndex());
    if (!entry.field) {
        qCWarning(QT_BRIDGE, "Property write: unable to find entry for: %s", mp.name());
        return;
    }

    // Store the userObject value in QProperty
    jobject fieldObj = JniContext::getEnv()->GetObjectField(javaObject, entry.field);
    JNIMethodInvoker::invokeMethod<void>(
        env, fieldObj, JNICache::qtPropertySetValueMethod(), userObject);

    if (fieldObj)
        env->DeleteLocalRef(fieldObj);
    if (userObject)
        env->DeleteLocalRef(userObject);
}

void QObjectJavaProxy::setPropertyDefaultValue(void **args, const QMetaType &mt)
{
    switch (mt.id()) {
    case QMetaType::Int:           *static_cast<int*>(args[0]) = 0; return;
    case QMetaType::LongLong:      *static_cast<qlonglong*>(args[0]) = 0; return;
    case QMetaType::Float:         *static_cast<float*>(args[0]) = 0.0f; return;
    case QMetaType::Double:        *static_cast<double*>(args[0]) = 0.0; return;
    case QMetaType::Bool:          *static_cast<bool*>(args[0]) = false; return;
    case QMetaType::QChar:         *static_cast<QChar*>(args[0]) = QChar{}; return;
    case QMetaType::SChar:         *static_cast<signed char*>(args[0]) = 0; return;
    case QMetaType::Short:         *static_cast<short*>(args[0]) = 0; return;
    case QMetaType::QString:       *static_cast<QString*>(args[0]) = QString{}; return;
    case QMetaType::QStringList:   *static_cast<QStringList*>(args[0]) = QStringList{}; return;
    case QMetaType::QVariantList:  *static_cast<QVariantList*>(args[0]) = QVariantList{}; return;
    case QMetaType::QVariantMap:   *static_cast<QVariantMap*>(args[0]) = QVariantMap{}; return;
    case QMetaType::QVariant:      *static_cast<QVariant*>(args[0]) = QVariant{}; return;
    case QMetaType::QUrl:          *static_cast<QUrl*>(args[0]) = QUrl{}; return;
    case QMetaType::QObjectStar:   *static_cast<QObject**>(args[0]) = nullptr; return;
    default:
        qCWarning(QT_BRIDGE, "Unsupported property type for default value: %s",  mt.name());
        return;
    }
}

int QObjectJavaProxy::addSlot(const QByteArray &signature, const QByteArray &returnType) const
{
    const auto id = m_dynamicMetaObject->addSlot(signature, returnType);
    return id;
}

int QObjectJavaProxy::addSignal(const QByteArray &signature) const
{
    return m_dynamicMetaObject->addSignal(signature);
}

int QObjectJavaProxy::addProperty(const QByteArray &name, const QtProperty &property) const
{
    return m_dynamicMetaObject->addProperty(name, property);
}

const QMetaObject *QObjectJavaProxy::metaObject() const { return m_dynamicMetaObject->metaObject(); }

// Performs the QML-initiated call, either a property access or a invokable method call.
// The calls are done on the user-side object
int QObjectJavaProxy::qt_metacall(QMetaObject::Call call, int index, void **args)
{
    int ret = 0;
    if (!m_userObject) {
        qCWarning(QT_BRIDGE, "qt_metacall: UserObject referred by QtObjectJavaProxy is null");
        return ret;
    }

    // First check if baseclass handlers the call already (QML 'children' property)
    int ix = QtProxyBase::qt_metacall(call, index, args);
    if (ix < 0)
        return ix;

    // The userObject reference is weak if Java owns the object; check if we need to promote
    // the reference before using it - the object may have been garbage collected already.
    // Garbage collection shouldn't primarily be noticed here though but through Java-side
    // cleaner mechanism, but let's be careful in case there are some fringe cases timing-wise
    JNIEnv *env = nullptr;
    jobject javaObjectRef = nullptr;

    if (m_ownedByQml) {
        // Strong global ref, can use as-is
        javaObjectRef = m_userObject;
    } else {
        // Promote weak reference to a local reference
        env = JniContext::getEnv();
        javaObjectRef = env->NewLocalRef(m_userObject);
        if (!javaObjectRef) {
            qCWarning(QT_BRIDGE) << "qt_metacall: UserObject already garbage collected!";
            m_userObject = nullptr;
            return ret;
        }
    }

    switch (call) {
    case QMetaObject::ReadProperty:
        ret = index - metaObject()->propertyCount();
        qtReadPropertyMetacall(javaObjectRef, index, args);
        break;
    case QMetaObject::WriteProperty:
        ret = index - metaObject()->propertyCount();
        qtWritePropertyMetacall(javaObjectRef, index, args);
        break;
    case QMetaObject::InvokeMetaMethod:
        ret = index - metaObject()->methodCount();
        qtMethodMetacall(javaObjectRef, index, args);
        break;
    default:
        break;
    }

    if (!m_ownedByQml && env && javaObjectRef)
        env->DeleteLocalRef(javaObjectRef);

    return ret;
}
