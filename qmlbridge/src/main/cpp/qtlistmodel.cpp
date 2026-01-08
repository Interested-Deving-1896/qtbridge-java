// Copyright (C) 2025 The Qt Company Ltd.
// SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only

#include "qtlistmodel.h"
#include "java_object.h"

QtListModel::QtListModel(jweak javaSideRef, QObject *parent) :
    QAbstractListModel(parent),
    m_nextRoleId(Qt::UserRole),
    m_javaSideRef(javaSideRef)
{
}

int QtListModel::rowCount(const QModelIndex &parent) const
{
    if (parent.isValid())
        return 0;
    return m_data.size();
}

// Called when data is changed by the application at QML-side
bool QtListModel::setData(const QModelIndex &index, const QVariant &value, int role)
{
    if (!index.isValid() || index.row() < 0 || index.row() >= m_data.size())
        return false;

    auto it = m_roleNames.constFind(role);
    if (it == m_roleNames.constEnd())
        return false;

    QVariantMap &item = m_data[index.row()];
    const QString key = QString::fromUtf8(it.value());
    if (item.value(key) != value) {
        item.insert(key, value);
        emit dataChanged(index, index, {role});
    }
    Utility::JNI::JavaObject::callMethod<void>(m_javaSideRef, "onDataChangedFromNative",
                                               static_cast<jint>(index.row()), static_cast<jint>(index.row()));
    return true;
}

QVariant QtListModel::data(const QModelIndex &index, int role) const
{
    if (!index.isValid() || index.row() < 0 || index.row() >= m_data.size())
        return QVariant();

    if (!m_roleNames.contains(role))
        return QVariant();
    const QByteArray &roleName = m_roleNames.value(role);
    const QVariantMap &item = m_data.at(index.row());

    return item.value(QString::fromUtf8(roleName));
}

QHash<int, QByteArray> QtListModel::roleNames() const
{
    return m_roleNames;
}

bool QtListModel::contains(const QVariantMap &item) const
{
    return m_data.contains(item);
}

int QtListModel::size() const
{
    return m_data.size();
}

void QtListModel::appendItem(const QVariantMap &item)
{
    // Automatically register any new roles based on the item's keys
    for (auto [key, _] : item.asKeyValueRange()) {
        QByteArray utf8Key = key.toUtf8();
        if (!(std::find(m_roleNames.cbegin(), m_roleNames.cend(), utf8Key) != m_roleNames.cend()))
            m_roleNames[m_nextRoleId++] = utf8Key;
    }

    beginInsertRows(QModelIndex(), m_data.size(), m_data.size());
    m_data.append(item);
    endInsertRows();

    Utility::JNI::JavaObject::callMethod<void>(m_javaSideRef, "onRowsInsertedFromNative",
                                               static_cast<jint>(m_data.size() - 1), static_cast<jint>(m_data.size() - 1));
}

void QtListModel::removeItem(const QVariantMap &item)
{
    for (int i = 0; i < m_data.size(); ++i) {
        if (m_data[i] == item) {
            removeItemAt(i);
            return;
        }
    }
}

void QtListModel::removeItemAt(int index)
{
    if (index >= 0 && index < m_data.size()) {
        beginRemoveRows(QModelIndex(), index, index);
        m_data.removeAt(index);
        endRemoveRows();
        Utility::JNI::JavaObject::callMethod<void>(m_javaSideRef, "onRowsRemovedFromNative",
                                                   static_cast<jint>(index), static_cast<jint>(index));
    }
}

void QtListModel::updateItemAt(int index, const QVariantMap &item)
{
    if (index >= 0 && index < m_data.size()) {
        m_data[index] = item;
        QModelIndex modelIndex = createIndex(index, 0);
        emit dataChanged(modelIndex, modelIndex);
        Utility::JNI::JavaObject::callMethod<void>(m_javaSideRef, "onDataChangedFromNative",
                                                   static_cast<jint>(index), static_cast<jint>(index));
    }
}

void QtListModel::reset()
{
    if (m_data.empty())
        return;
    beginResetModel();
    m_data.clear();
    endResetModel();

    Utility::JNI::JavaObject::callMethod<void>(m_javaSideRef, "onResetFromNative");
}

void QtListModel::replaceAll(const QList<QVariantMap> &items)
{
    beginResetModel();
    m_data = items;

    // Rebuild role names from new items' keys
    m_roleNames.clear();
    m_nextRoleId = Qt::UserRole;
    for (const QVariantMap &item : std::as_const(m_data)) {
        for (auto [key, _] : item.asKeyValueRange()) {
            const QByteArray keyUtf8 = key.toUtf8();
            if (!(std::find(m_roleNames.cbegin(), m_roleNames.cend(), keyUtf8) != m_roleNames.cend()))
                m_roleNames[m_nextRoleId++] = keyUtf8;
        }
    }
    endResetModel();

    Utility::JNI::JavaObject::callMethod<void>(m_javaSideRef, "onResetFromNative");
}

QVariantMap QtListModel::getItem(int index) const
{
    if (index >= 0 && index < m_data.size())
        return m_data.at(index);
    return QVariantMap();
}

QVariantList QtListModel::getAllItems() const
{
    QVariantList result;
    for (const QVariantMap &item : m_data)
        result.append(item);
    return result;
}

void QtListModel::addRoleName(const QString &roleName)
{
    if (!roleName.isEmpty()
        && !(std::find(m_roleNames.cbegin(), m_roleNames.cend(), roleName.toUtf8())
             != m_roleNames.cend())) {
        m_roleNames[m_nextRoleId++] = roleName.toUtf8();
    }
}
