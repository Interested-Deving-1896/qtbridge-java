/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.exception;

/**
 * Exception thrown when a native library cannot be loaded.
 */
public class NativeLibraryLoadException extends RuntimeException {
    private final String libraryName;

    public NativeLibraryLoadException(String libraryName, String message) {
        super(message);
        this.libraryName = libraryName;
    }

    public NativeLibraryLoadException(String libraryName, String message, Throwable cause) {
        super(message, cause);
        this.libraryName = libraryName;
    }

    @Override
    public String getMessage() {
        return "Failed to load native library '" + libraryName + "': " + super.getMessage();
    }
}
