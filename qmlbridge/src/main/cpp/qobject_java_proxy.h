/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#ifndef QOBJECT_JAVA_PROXY_H
#define QOBJECT_JAVA_PROXY_H

#include "dynamic_metaobject.h"
#include "jni_utilities.h"
#include "qt_property.h"

#include <QtCore/qbytearray.h>
#include <QtQml/qqmllist.h>
#include <QtQml/qqmlparserstatus.h>

#include <jni.h>

// QtProxyBase is an intermediate class that serves two purposes:
// 1) it implements support DefaultProperty
// 2) it's a static baseclass for the proxy object. It seems that since
//    the proxy class's metaobject is built at runtime, a qobject_cast<>
//    does not work reliably across Qt versions. This forces us to use
//    reinterpret_cast. But with a stable metaboject we can use
//    qobject_cast<QtProxyBase*> when needed.
class QtProxyBase : public QObject {
    Q_OBJECT
    Q_CLASSINFO("DefaultProperty", "children")
    Q_PROPERTY(QQmlListProperty<QObject> children READ children CONSTANT)

public:
    explicit QtProxyBase(QObject *parent = nullptr): QObject (parent) {}

    QQmlListProperty<QObject> children() {
        return QQmlListProperty<QObject>(this, &m_children);
    }

    virtual jobject userObjectLocalRef() = 0;

private:
    QList<QObject *> m_children;
};

class QObjectJavaProxy final : public QtProxyBase, public QQmlParserStatus
{
public:
    explicit QObjectJavaProxy(qint64 cacheKey, QObject *parent = nullptr);
    ~QObjectJavaProxy() override;

    void classBegin() override;
    void componentComplete() override;

    static QObjectJavaProxy *fromHandle(jlong h) {
        return reinterpret_cast<QObjectJavaProxy*>(h);
    }

    static QObjectJavaProxy *fromObject(jobject qObject) {
        return fromHandle(JNIUtilities::getNativeHandleFromObject(qObject));
    }

    qint64 cacheKey() const
    {
        return m_cacheKey;
    }

    int addSlot(const QByteArray &signature, const QByteArray &returnType) const;
    int addSignal(const QByteArray &signature) const;
    int addProperty(const QByteArray &name, const QtProperty &property) const;

    const QMetaObject *metaObject() const override;
    int qt_metacall(QMetaObject::Call call, int index, void **args) override;

    void registerUserObject(jobject userObjectLocalRef, bool ownedByQML);
    void registerQtObject(jobject userObject);

    jobject userObjectLocalRef() override;

private:
    void qtMethodMetacall(const jobject javaObject, const int methodIndex, void **args);
    void qtWritePropertyMetacall(const jobject javaObject, const int propertyIndex, void **args);
    void qtReadPropertyMetacall(const jobject javaObject, const int propertyIndex, void **args);

    void readItemModelProperty(const jobject javaObject, const QMetaProperty &mp, void **args);

    void readQmlRegistrableProperty(const jobject javaObject, const QMetaProperty &mp,
                                    void **args);
    void writeQmlRegistrableProperty(const jobject javaObject, const QMetaProperty &mp,
                                     void **args);
    static void setPropertyDefaultValue(void **args, const QMetaType &mt);


    std::unique_ptr<::DynamicMetaObject> m_dynamicMetaObject;
    bool m_ownedByQml = true; // Indicates whether object is owned by QML or Java
    // Ownerhsip of the user object dictates the type of userObject reference:
    //
    // If QML/JS owns: strong. Storing a strong reference keeps the object alive until
    // QML destroys this proxy object. This avoids the untimely Java-side garbage collection.
    //
    // If Java owns: weak. Keeping the reference weak allows Java-side garbage
    // collector to kick in when needed. QtObject will need to listen for the
    // userObject cleanup event and dispose itself when the userObject is gone.
    //
    // QtObject OTOH is always a strong reference.
    jobject m_userObject{}; // either weak or strong global ref
    jobject m_qtObject{};   // always a strong global ref
    // used for caching the proxy
    qint64 m_cacheKey{};
};
#endif // Q_OBJECT_JAVA_PROXY
