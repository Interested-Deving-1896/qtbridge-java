/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

#include "jni_dynamic_metaobject.h"

#include "qt_property.h"

#include <QtCore/qbytearray.h>

using namespace Qt::StringLiterals;

static bool checkSignature(const QByteArray &signature)
{
    const auto openParen = signature.indexOf('(');
    const auto closingParen = signature.lastIndexOf(')');
    const bool ok = openParen != -1 && closingParen != -1 && openParen < closingParen;
    if (!ok) {
        qDebug() << "MetaObjectBuilder::addMethod: Invalid method signature provided for " +
                            signature;
        return false;
    }
    return ok;
}

JniDynamicMetaObject::JniDynamicMetaObject(const char *className, const QMetaObject *metaObject)
{
    m_baseObject = metaObject;
    m_builder = new QMetaObjectBuilder();
    m_builder->setClassName(className);
    m_builder->setSuperClass(metaObject);
}

JniDynamicMetaObject::~JniDynamicMetaObject()
{
    delete m_builder;
}

QMetaPropertyBuilder JniDynamicMetaObject::createProperty(const QByteArray &name,
                                                       const QtProperty &value)
{
    int propertyNotifyId = getPropertyNotifyId(value.notifySignature);
    if (propertyNotifyId >= 0)
        propertyNotifyId -= m_baseObject->methodCount();

    auto *builder = provideBuilder();
    const auto metaType = QMetaType::fromName(value.cppType);
    return builder->addProperty(name, value.cppType, metaType, propertyNotifyId);
}

int JniDynamicMetaObject::indexOfMethod(QMetaMethod::MethodType mtype,
                                     const QByteArray &signature) const
{
    int result = -1;

    switch (mtype) {
    case QMetaMethod::Signal:
        result = m_builder->indexOfSignal(signature);
        break;
    case QMetaMethod::Slot:
        result = m_builder->indexOfSlot(signature);
        break;
    case QMetaMethod::Constructor:
        result = m_builder->indexOfConstructor(signature);
        break;
    case QMetaMethod::Method:
        result = m_builder->indexOfMethod(signature);
        break;
    }
    if (result >= 0)
        return result + m_baseObject->methodCount();
    return result;
}

int JniDynamicMetaObject::indexOfProperty(const QByteArray &name) const
{
    if (m_builder) {
        const int result = m_builder->indexOfProperty(name);
        if (result >= 0)
            return m_baseObject->propertyCount() + result;
    }
    return m_baseObject->indexOfProperty(name);
}

int JniDynamicMetaObject::getPropertyNotifyId(const QByteArray &signature) const
{
    int notifyId = -1;
    if (!signature.isEmpty()) {
        notifyId = indexOfMethod(QMetaMethod::Signal, signature);
    }
    return notifyId;
}

QMetaObjectBuilder *JniDynamicMetaObject::provideBuilder()
{
    if (!m_builder) {
        m_builder = new QMetaObjectBuilder();
        m_builder->setClassName(m_baseObject->className());
        m_builder->setSuperClass(m_baseObject);
    }
    return m_builder;
}

int JniDynamicMetaObject::addSlot(const QByteArray &signature, const QByteArray &returnType)
{
    if (!checkSignature(signature))
        return -1;
    QMetaMethodBuilder methodBuilder = provideBuilder()->addSlot(signature);
    if (!returnType.isEmpty() && returnType != "void"_ba)
        methodBuilder.setReturnType(returnType);
    return m_baseObject->methodCount() + methodBuilder.index();
}

int JniDynamicMetaObject::addSignal(const QByteArray &signature)
{
    if (!checkSignature(signature))
        return -1;
    return m_baseObject->methodCount() + provideBuilder()->addSignal(signature).index();
}

int JniDynamicMetaObject::addProperty(const QByteArray &name, const QtProperty &value)
{
    int index = indexOfProperty(name);
    if (index != -1)
        return index;
    auto newProperty = createProperty(name, value);

    newProperty.setReadable(value.isReadable);
    newProperty.setWritable(value.isWriteable);
    newProperty.setConstant(value.isConstant);
    index = newProperty.index() + m_baseObject->propertyCount();

    return index;
}

const QMetaObject *JniDynamicMetaObject::metaObject() const
{
    return m_builder->toMetaObject();
}

void JniDynamicMetaObject::dumpQObjectMeta(const QObject *obj)
{
    if (!obj) {
        qDebug() << "dumpQObjectMeta: null object";
        return;
    }

    const QMetaObject *mo = obj->metaObject();
    qDebug().noquote() << "=== Meta dump start for" << mo->className() << "(" << obj << ") ===";

    auto printMethod = [](const QMetaMethod &m) {
        // e.g. "void valueChanged(int)"
        const QByteArray sig = m.methodSignature();
        auto ret = m.returnMetaType().name();
        QString kind;
        switch (m.methodType()) {
        case QMetaMethod::Signal:    kind = "signal"_L1;    break;
        case QMetaMethod::Slot:      kind = "slot"_L1;      break;
        case QMetaMethod::Method:    kind = "invokable"_L1; break;
        default:                     kind = "method"_L1;    break;
        }
        qDebug().noquote() << "  [%1] %2 %3"_L1
                                  .arg(kind, QString::fromLatin1(ret), QString::fromLatin1(sig));
    };

    // Walk the inheritance chain: this class, then its superclasses
    for (const QMetaObject *m = mo; m; m = m->superClass()) {
        qDebug().noquote() << "\n-- Declared in" << m->className() << "--";

        // Properties declared at this level
        if (m->propertyCount() > m->propertyOffset()) {
            qDebug() << " Properties:";
            for (int i = m->propertyOffset(); i < m->propertyCount(); ++i) {
                const QMetaProperty p = m->property(i);
                QString flags;
                if (p.isReadable())   flags += "R"_L1;
                if (p.isWritable())   flags += "W"_L1;
                if (p.isConstant())   flags += "C"_L1;
                if (p.isFinal())      flags += "F"_L1;
                if (p.isDesignable()) flags += "D"_L1;
                if (p.isScriptable()) flags += "S"_L1;
                if (p.isStored())     flags += "T"_L1;
                if (p.isUser())       flags += "U"_L1;

                QString line = "  %1 %2  [%3]"_L1.arg(QString::fromLatin1(p.typeName()),
                                                      QString::fromLatin1(p.name()))
                                                 .arg(flags.isEmpty() ? "-"_L1 : flags);

                if (p.hasNotifySignal()) {
                    const QMetaMethod n = p.notifySignal();
                    line += "  notify: %1"_L1.arg(QString::fromLatin1(n.methodSignature()));
                }
                qDebug().noquote() << line;
            }
        }

        // Methods declared at this level (split signals vs others for readability)
        if (m->methodCount() > m->methodOffset()) {
            // Signals
            bool anySignals = false;
            for (int i = m->methodOffset(); i < m->methodCount(); ++i) {
                const QMetaMethod mm = m->method(i);
                if (mm.methodType() == QMetaMethod::Signal) {
                    if (!anySignals) { qDebug() << " Signals:"; anySignals = true; }
                    printMethod(mm);
                }
            }
            // Non-signal invokables (slots + INVOKABLEs)
            bool anyOthers = false;
            for (int i = m->methodOffset(); i < m->methodCount(); ++i) {
                const QMetaMethod mm = m->method(i);
                if (mm.methodType() != QMetaMethod::Signal) {
                    if (!anyOthers) { qDebug() << " Methods:"; anyOthers = true; }
                    printMethod(mm);
                }
            }
        }
    }
    qDebug().noquote() << "\n=== End meta dump for " << mo->className() << "===";
}
