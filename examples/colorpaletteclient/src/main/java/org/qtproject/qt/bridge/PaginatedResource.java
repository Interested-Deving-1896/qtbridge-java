/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.core.QtProperty;
import java.util.List;
import java.util.Map;

@QMLRegistrable(includeSuper = false)
public class PaginatedResource extends AbstractResource {
    public QtProperty<List<Map<String, Object>>> data = new QtProperty<>(null);
    public QtProperty<Integer> page = new QtProperty<>(1);
    public QtProperty<Integer> pages = new QtProperty<>(0);
    public QtProperty<String> path  = new QtProperty<>(null);

    {
        page.onValueChanged(() -> {
            if (page.getValue() < 1)
                return;
            refreshCurrentPage();
        });
    }

    public void refreshCurrentPage() {
        getJson(path.getValue() + "?page=" + page.getValue())
                .thenAccept(response -> {
                    if (response.ok() && response.json != null) {
                        @SuppressWarnings("unchecked")
                        var items = (List<Map<String,Object>>) response.json.get("data");
                        Number totalPages = (Number) response.json.getOrDefault("total_pages", 1);
                        data.setValue(items);
                        pages.setValue(totalPages.intValue());
                    } else {
                        if (page.getValue() != 1) {
                            // A failed refresh. If we weren't on page 1, try that.
                            // Last resource on currentPage might have been deleted, causing a failure
                            page.setValue(1);
                        } else {
                            // Refresh failed, and we're already on page 1 => clear data
                            page.setValue(0);
                            data.setValue(null);
                        }
                    }
                });
    }

    public void update(Map<String, Object> data, int id) {
        putJson(path.getValue() + "/" + id, data)
                .thenAccept(response -> {
                    if (response.ok())
                        refreshCurrentPage();
                });
    }

    public void add(Map<String, Object> data) {
        postJson(path.getValue(), data)
                .thenAccept(response -> {
                    if (response.ok())
                        refreshCurrentPage();
                });
    }

    public void remove(int id) {
        deleteJson(path.getValue() + "/" + id)
                .thenAccept(response -> {
                    if (response.ok())
                       refreshCurrentPage();
                });
    }
}
