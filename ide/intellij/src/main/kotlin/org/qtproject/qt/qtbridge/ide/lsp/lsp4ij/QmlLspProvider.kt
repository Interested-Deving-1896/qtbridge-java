/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.qtbridge.ide.lsp.lsp4ij

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import org.qtproject.qt.qtbridge.ide.utils.resolveQmlLsCommandLine
import java.io.InputStream
import java.io.OutputStream

internal class QmlLspIJProvider(private val project: Project) : OSProcessStreamConnectionProvider() {
    override fun start() {
        commandLine = project.resolveQmlLsCommandLine() ?: return
        super.start()
    }
}

internal object DisabledLspProvider : StreamConnectionProvider {
    override fun start() = Unit
    override fun stop() = Unit
    override fun getInputStream(): InputStream = InputStream.nullInputStream()
    override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()
}
