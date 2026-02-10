/*
 * Copyright (C) 2025 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.net.http.HttpResponse;

public abstract class AbstractResource {
    private HttpClient m_http;
    private URI m_baseUri;
    private ObjectMapper m_json;
    private AuthorizationContext m_authorizationContext;

    public interface AuthorizationContext {
        String getAuthorizationToken();
        void setAuthorizationToken(String token);
    };
    public void setAuthorizationContext(AuthorizationContext authorizationContext) {
        m_authorizationContext = authorizationContext;
    }

    public final void setHttpClient(@NotNull HttpClient http, @NotNull ObjectMapper json) {
        m_http = Objects.requireNonNull(http, "http");
        m_json = Objects.requireNonNull(json, "json");
    }

    public void setBaseUri(URI baseUri) {
        m_baseUri = baseUri;
    }

    // JSON response wrapper
    public static final class JsonResult {
        public final int status;
        public final Map<String, Object> json;
        public final String error;
        public JsonResult(int status, Map<String, Object> body, String error) {
            this.status = status; this.json = body; this.error = error;
        }
        public boolean ok() { return status / 100 == 2; }
    }

    // HTTP GET
    public CompletableFuture<JsonResult> getJson(String path) {
        HttpRequest req = request(path).GET().build();
        return sendJson(req);
    }

    // HTTP POST
    public CompletableFuture<JsonResult> postJson(String path, Map<String, Object> payload) {
        try {
            String json = "{}";
            if (payload != null)
                json = m_json.writeValueAsString(payload);
            HttpRequest req = request(path)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return sendJson(req);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    // HTTP DELETE
    public CompletableFuture<JsonResult> deleteJson(String path) {
        HttpRequest req = request(path)
                .DELETE()
                .build();
        return sendJson(req);
    }

    // HTTP PUT
    public CompletableFuture<JsonResult> putJson(String path, Map<String, Object> payload) {
        try {
            String json = m_json.writeValueAsString(payload);
            HttpRequest req = request(path)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return sendJson(req);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    protected void setAuthorizationToken(String token) {
        m_authorizationContext.setAuthorizationToken(token);
    }

    private CompletableFuture<JsonResult> sendJson(HttpRequest req) {
        return m_http
                .sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    int status = resp.statusCode();
                    String body = resp.body();
                    if (status / 100 == 2) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parsed = m_json.readValue(body, Map.class);
                            return new JsonResult(status, parsed, null);
                        } catch (Exception e) {
                            return new JsonResult(status, null, body);
                        }
                    } else {
                        return new JsonResult(status, null, body);
                    }
                });
    }

    private URI resolve(String path) {
        String resolved;
        if (m_baseUri != null) {
            String base = m_baseUri.toString();
            if (!base.endsWith("/")) base = base + "/";
            resolved = URI.create(base).resolve(path).toString();
        } else {
            resolved = path; // fallback: no base URI yet
        }
        return URI.create(resolved);
    }

    private HttpRequest.Builder request(String path) {
        URI resolved = resolve(path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(resolved)
                .timeout(Duration.ofSeconds(5));
        if (m_authorizationContext.getAuthorizationToken() != null)
            builder.header("token", m_authorizationContext.getAuthorizationToken());
        return builder;
    }
}
