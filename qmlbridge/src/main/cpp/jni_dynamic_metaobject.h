/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#ifndef JNI_DYNAMIC_METAOBJECT_H
#define JNI_DYNAMIC_METAOBJECT_H

#include <private/qmetaobjectbuilder_p.h>

class QByteArray;
class QObject;
class QMetaObject;
class QtProperty;

class JniDynamicMetaObject
{
public:
    JniDynamicMetaObject(const char *className, const QMetaObject *metaObject);
    ~JniDynamicMetaObject();

    int addSlot(const QByteArray &signature, const QByteArray &returnType);
    int addSignal(const QByteArray &signature);
    int addProperty(const QByteArray &name, const QtProperty &value);

    const QMetaObject *metaObject() const;
    // Development-time helper that prints the metaobject
    static void dumpQObjectMeta(const QObject *obj);

private:
    QMetaPropertyBuilder createProperty(const QByteArray &propertyName, const QtProperty &property);
    int getPropertyNotifyId(const QByteArray &signature) const;

    int indexOfMethod(QMetaMethod::MethodType mtype, const QByteArray &signature) const;
    int indexOfProperty(const QByteArray &name) const;

    QMetaObjectBuilder *provideBuilder();

    const QMetaObject *m_baseObject = nullptr;
    QMetaObjectBuilder *m_builder = nullptr;
};

#endif // JNI_DYNAMIC_METAOBJECT_H
