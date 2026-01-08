/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR GPL-3.0-only
 */

package org.qtproject.qt.bridge.autotest;

// Java-side TestBackend calls these to inform QML-side of the
// received QtListModel observer notifications => this way we
// can autotest Java-side observer notifications
public interface QMLCallback {
    void modelInserted(int start, int count);
    void modelRemoved(int start, int count);
    void modelChanged(int start, int count);
    void modelReset();
    void modelSizeChanged();
}
