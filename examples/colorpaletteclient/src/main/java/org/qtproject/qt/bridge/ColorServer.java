/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class ColorServer {

    private HttpServer m_server = null;

    public ColorServer() throws IOException {
        m_server = HttpServer.create(new InetSocketAddress("127.0.0.1", 49426), 0);
        m_server.createContext("/api/unknown", this::handleUnknown);
        m_server.createContext("/api/users", this::handleUsers);
        m_server.createContext("/api/login", this::handleLogin);
        m_server.createContext("/api/logout", this::handleLogout);
    }

    public void start() throws IOException {
        System.out.println("Starting Bundled Java Server at :" + m_server.getAddress());
        m_server.start();
    }

    public void stop() {
        if (m_server == null)
            return;
        m_server.stop(0);
    }

    private void handleUnknown(HttpExchange exchange) throws IOException {
        System.out.println("TODO handleUnknown()");
        exchange.sendResponseHeaders(501, 0);
        exchange.close();
    }

    private void handleUsers(HttpExchange exchange) throws IOException {
        System.out.println("TODO handleUsers()");
        exchange.sendResponseHeaders(501, 0);
        exchange.close();
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        System.out.println("TODO handleLogin()");
        exchange.sendResponseHeaders(501, 0);
        exchange.close();
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        System.out.println("TODO handleLogout()");
        exchange.sendResponseHeaders(501, 0);
        exchange.close();
    }
}
