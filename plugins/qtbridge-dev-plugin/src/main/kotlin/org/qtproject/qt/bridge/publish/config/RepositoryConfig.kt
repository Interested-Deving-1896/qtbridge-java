/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.publish.config

import java.net.URI

internal data class RepositoryConfig(
    val name: String,
    val url: URI,
    val credentials: Credentials? = null
) {
    data class Credentials(val username: String?, val password: String?)
}
