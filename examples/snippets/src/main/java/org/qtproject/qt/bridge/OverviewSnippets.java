/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge.docs;

/**
 * <h2>Example</h2>
 * <p>This documentation illustrates a simple but reasonably complete example application.</p>
 *
 * <h3>Registrable class on Java-side</h3>
 * <p>First we create a Java-class and annotate it appropriately. This class,
 * {@code MyType}, will be a creatable type in QML.</p>
 * {@snippet file="snippets/src/main/java/org/qtproject/qt/bridge/MyType.java"
 *           region="qmlregistrable-fullclass-creatable"
 *           lang=java}
 *
 * <p>For illustrative purposes, let's also register a singleton type that
 * implements the same interface (omitted here):</p>
 * {@snippet file = "snippets/src/main/java/org/qtproject/qt/bridge/MySingleton.java"
 *           region = "qmlregistrable-singleton"
 *           lang = java}
 *
 * <h3>QML-side usage</h3>
 * <p>Next create a simple QML application. It illustrates both creatable and singleton type uses:</p>
 * {@snippet file="snippets/src/main/qml/Main.qml"
 *           region="qmlapplication-full"
 *           lang=java}
 */
public final class OverviewSnippets {
    private OverviewSnippets() {} // doc-snippets-only holder
}
