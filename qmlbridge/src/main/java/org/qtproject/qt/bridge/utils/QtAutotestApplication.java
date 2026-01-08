/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utils;
import org.qtproject.qt.bridge.core.QtAbstractApplication;

// This class is intended for autotests
public class QtAutotestApplication extends QtAbstractApplication
{
    public static void execute(String[] args) {
        int exitCode = nativeExecuteTest(args);
        System.exit(exitCode);
    }

    private static native int nativeExecuteTest(String[] args);
}
