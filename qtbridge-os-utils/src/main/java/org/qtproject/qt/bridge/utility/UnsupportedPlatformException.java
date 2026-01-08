/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utility;

class UnsupportedPlatformException extends RuntimeException {

    UnsupportedPlatformException(String message) {
        super(message);
    }

    UnsupportedPlatformException(String message, Throwable cause) {
        super(message, cause);
    }
}
