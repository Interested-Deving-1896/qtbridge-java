/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.publish

import org.gradle.api.Project

internal data class PublishContext(
    val project: Project,
    val extension: QtBridgePublishingExtension,
    val environment: PublishEnvironment
)
