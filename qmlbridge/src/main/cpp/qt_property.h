/*
 * Copyright (C) 2024 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */
#ifndef QT_PROPERTY_H
#define QT_PROPERTY_H

#include <QtCore/qbytearray.h>

struct QtProperty
{
    QByteArray cppType;
    bool isWriteable;
    bool isReadable;
    bool isConstant;
    QByteArray notifySignature;
};
#endif // QT_PROPERTY
