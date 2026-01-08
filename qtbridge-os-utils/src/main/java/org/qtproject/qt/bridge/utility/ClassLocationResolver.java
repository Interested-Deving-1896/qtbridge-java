/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.utility;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

public final class ClassLocationResolver {
    public static Path getClassLocation(Class<?> clazz) {
        try {
            CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }
            URL location = codeSource.getLocation();
            if (location == null)
                return null;
            Path path = Paths.get(location.toURI());
            // If it's a JAR file, return its parent directory
            if (path.toFile().isFile())
                return path.getParent();
            // Otherwise it's already a directory (classes folder)
            return path;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
