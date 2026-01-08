/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge;

import org.qtproject.qt.bridge.core.QtProperty;
import org.qtproject.qt.bridge.annotations.QMLComplete;
import org.qtproject.qt.bridge.annotations.QMLRegistrable;
import org.qtproject.qt.bridge.core.QtQmlChildren;
import java.net.URI;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;

@QMLRegistrable
public class RestService implements AbstractResource.AuthorizationContext {
    public QtProperty<URI> url = new QtProperty<>(null);
    public QtProperty<Boolean> sslSupported = new QtProperty<>(true);
    {
        url.onValueChanged(() -> {
            List<AbstractResource> children = QtQmlChildren.children(this, AbstractResource.class);
            URI uri = url.getValue();
            for (AbstractResource child : children)
                child.setBaseUri(uri);
        });
    }

    private String m_authorizationToken;
    @Override
    public String getAuthorizationToken() {
        return m_authorizationToken;
    }
    @Override
    public void setAuthorizationToken(String token) {
        m_authorizationToken = token;
    }

    private HttpClient m_httpClient;
    private ObjectMapper m_json;

    @QMLComplete
    void onComplete() {
        List<AbstractResource> children = QtQmlChildren.children(this, AbstractResource.class);
        // Create shared resources and configure the resources with them
        m_httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_2)
                .build();
        m_json = new ObjectMapper();

        for (AbstractResource child : children) {
            child.setBaseUri(url.getValue());
            child.setHttpClient(m_httpClient, m_json);
            child.setAuthorizationContext(this);
        }
    }
}
