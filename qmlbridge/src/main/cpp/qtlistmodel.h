/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */
#ifndef QTLISTMODEL_H
#define QTLISTMODEL_H

#include <QAbstractListModel>
#include "jni.h"

class QtListModel : public QAbstractListModel
{
public:
    explicit QtListModel(jweak javaSideRef, QObject *parent = nullptr);

    int rowCount(const QModelIndex &parent = QModelIndex()) const override;
    QVariant data(const QModelIndex &index, int role = Qt::DisplayRole) const override;
    QHash<int, QByteArray> roleNames() const override;

    bool setData(const QModelIndex &index, const QVariant &value, int role = Qt::EditRole) override;

    int size() const;
    bool contains(const QVariantMap &item) const;
    void appendItem(const QVariantMap &item);
    void removeItem(const QVariantMap &item);
    void removeItemAt(int index);
    void updateItemAt(int index, const QVariantMap &item);
    void replaceAll(const QList<QVariantMap> &items);
    void reset();
    QVariantMap getItem(int index) const;
    QVariantList getAllItems() const;
    void addRoleName(const QString &roleName);

private:
    QList<QVariantMap> m_data;
    QHash<int, QByteArray> m_roleNames;
    int m_nextRoleId;
    jweak m_javaSideRef;
};

#endif // QTLISTMODEL_H
