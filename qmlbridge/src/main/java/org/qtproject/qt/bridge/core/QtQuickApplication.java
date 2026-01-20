/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR LGPL-3.0-only
 */

package org.qtproject.qt.bridge.core;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;

import java.io.File;

/**
 * Minimal Qt Quick application runner for the Java -> Qt/QML bridge.
 *
 * <p>This class wires up a Qt application, loads a QML entry point, and runs the Qt event loop.
 * It builds on the bootstrap in {@link QtAbstractApplication}, which loads the native bridge and
 * registers {@link QMLRegistrable @QMLRegistrable} types. Only one QtQuickApplication
 * can exist per application. QtQuickApplications cannot be restarted. On {@code macOS} the
 * application must be started on the first thread ({@code -XstartOnFirstThread}). </p>
 *
 * <strong>Examples (Kotlin and Java)</strong>
 * {@snippet file = "snippets/src/main/kotlin/org/qtproject/qt/bridge/Main.kt"
 *           region = "qtquickapplication-main-kotlin"
 *           lang = kotlin}
 * {@snippet file = "minimalapp/src/main/java/org/qtproject/qt/bridge/Main.java"
 *           region = "qtquickapplication-main-java"
 *           lang = java}
 *
 * <h2>Threading</h2>
 * <p>{@link #execute()} runs the Qt event loop and blocks the calling thread (typically {@code main}).
 * Interactions from QML into Java occur on the Qt thread. If your Java code needs to update state
 * on another thread, forward accordingly. TODO this must be verified for correctness</p>
 *
 * @see QtAbstractApplication
 * @since 1.0
 */
public class QtQuickApplication extends QtAbstractApplication {
    private boolean m_qmlLoaded = false;

    /**
     * Creates a Qt Quick application and initializes the native bridge.
     *
     * <p>Typically you pass the {@code args} from your {@code main(String[])} unchanged.
     * Use an empty array if you have no arguments.</p>
     *
     * @param argv command-line arguments to forward to the Qt application (use an empty array if none)
     * @throws IllegalArgumentException if argv is null
     * @throws RuntimeException if native application initialization fails
     */
    public QtQuickApplication(final String[] argv) {
        nativeCreateApplication(argv);
    }

    /**
     * Enters the Qt event loop and executes the application.
     *
     * <p>Before starting the event loop, this method ensures QML has been loaded using this priority:</p>
     * <ol>
     *   <li>QML already loaded via {@link #loadQmlContent(String)}</li>
     *   <li>QML already loaded via {@link #loadQmlFile(String)}</li>
     *   <li>System property {@code qt.main.qml} (loaded automatically if no QML loaded yet)</li>
     * </ol>
     *
     * <p>This method blocks until {@link #quit()} is called or the application exits from QML.
     * <strong>Note:</strong> Applications cannot be restarted after execution completes.</p>
     *
     * @throws IllegalStateException if no QML source is available (no load methods called and system property not set)
     * @throws RuntimeException if the application has been already released or if called from wrong thread
     */
    public void execute() {
        if (!m_qmlLoaded) {
            String propertyFile = System.getProperty("qt.main.qml");
            if (propertyFile == null || propertyFile.isEmpty()) {
                throw new IllegalStateException("No QML source specified. Use loadQmlFile(), loadQmlContent(), or set -Dqt.main.qml=<path>");
            }
            nativeLoadQMLFile(propertyFile);
        }
        nativeExecuteApplication();
    }

    /**
     * Loads a QML file from the specified path.
     *
     * <p>This method should be called before {@link #execute()}. The QML file is loaded
     * immediately by this method.</p>
     *
     * @param qmlFilePath path to the QML file
     * @throws IllegalArgumentException if qmlFilePath is null or empty
     * @throws IllegalStateException if called after application has executed, if QML has already been loaded,
     *                              File does not exist or if called from wrong thread
     */
    public void loadQmlFile(final String qmlFilePath) {
        if (qmlFilePath == null || qmlFilePath.isEmpty()) {
            throw new IllegalArgumentException("QML file path cannot be null or empty");
        }
        File qmlFile = new File(qmlFilePath);
        if (!qmlFile.exists())
            throw new IllegalArgumentException("QML file does not exist");
        m_qmlLoaded = nativeLoadQMLFile(qmlFilePath);
        if (!m_qmlLoaded)
            throw new IllegalStateException("Failed to load QML file");
    }

    /**
     * Loads QML content directly from a string.
     *
     * <p>This method should be called before {@link #execute()}. The QML content is loaded
     * immediately by this method.</p>
     *
     * @param qmlContent the QML content as a string
     * @throws IllegalArgumentException if qmlContent is null or empty
     * @throws IllegalStateException if called after application has executed, if QML has already been loaded,
     *                          or if called from wrong thread
     */
    public void loadQmlContent(final String qmlContent) {
        if (qmlContent == null || qmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("QML content cannot be null or empty");
        }
        m_qmlLoaded = nativeLoadQMLContent(qmlContent);
        if (!m_qmlLoaded)
            throw new IllegalStateException("Failed to load QML content");
    }

    /**
     * Requests application shutdown.
     *
     * <p>Causes the Qt event loop (started by {@link #execute()}) to exit. Once the loop
     * terminates, control returns to the caller of {@code execute()}.
     * <strong>Note:</strong> applications cannot be restarted.</p>
     */
    public void quit() {
        nativeQuitApplication();
    }
    private static native void nativeCreateApplication(String[] argv);
    /**
     * Executes the native Qt application event loop.
     * @throws IllegalStateException if called from wrong thread
     */
    private static native void nativeExecuteApplication();
    /**
     * Quits the native Qt application event loop.
     * @throws IllegalStateException if called from wrong thread
     */
    private static native void nativeQuitApplication();

    /**
     * Loads a QML file in the native layer.
     * @param fileName the path to the QML file
     * @return true if loading succeeded, false otherwise
     * @throws IllegalStateException if already executed, QML already loaded or called from wrong thread
     */
    private static native boolean nativeLoadQMLFile(final String fileName);

    /**
     * Loads QML content in the native layer.
     * @param content the QML content as a string
     * @return true if loading succeeded, false otherwise
     * @throws IllegalStateException if already executed, QML already loaded or called from wrong thread
     */
    private static native boolean nativeLoadQMLContent(final String content);
}
