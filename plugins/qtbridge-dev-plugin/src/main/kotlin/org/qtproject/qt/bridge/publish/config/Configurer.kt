/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.publish.config

import org.qtproject.qt.bridge.publish.PublishContext

internal interface Configurer{
    fun configure(context: PublishContext)
}
