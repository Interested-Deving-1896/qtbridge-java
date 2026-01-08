/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge;

import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.core.QtProperty;
import java.util.Map;

@QMLRegistrable
public class BasicLogin extends AbstractResource {
    public QtProperty<String> user = new QtProperty<>(null);
    public QtProperty<Boolean> loggedIn = new QtProperty<>(false);
    public QtProperty<String> loginPath = new QtProperty<>(null);
    public QtProperty<String> logoutPath = new QtProperty<>(null);

    public void login(Map<String, Object> data) {
        postJson(loginPath.getValue(), data)
                .thenAccept(response -> {
                    if (response.ok() && response.json.containsKey("token")) {
                        String userEmail = "";
                        if (data.containsKey("email"))
                            userEmail = data.get("email").toString();
                        String userToken = response.json.get("token").toString();
                        user.setValue(userEmail);
                        setAuthorizationToken(userToken);
                        loggedIn.setValue(true);
                    } else {
                        user.setValue(null);
                        setAuthorizationToken(null);
                        loggedIn.setValue(false);
                    }
                });
    }

    public void logout() {
        postJson(logoutPath.getValue(), null)
                .thenAccept(response -> {
                    user.setValue(null);
                    setAuthorizationToken(null);
                    loggedIn.setValue(false);
                });
    }
}
