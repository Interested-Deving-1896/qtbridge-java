/*
 * Copyright (C) 2026 The Qt Company Ltd.
 * SPDX-License-Identifier: LicenseRef-Qt-Commercial OR BSD-3-Clause
 */

package org.qtproject.qt.bridge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Simple single-threaded HTTP server for colorpaletteclient.
// There are no long or I/O bound operations and this should suffice for
// the example's purposes (hence no particular concurrency protection either).
public class ColorServer {

    private final HttpServer m_server;
    // Resource (users and colors) containers
    private final List<Map<String, Object>> m_users = new ArrayList<>();
    private final List<Map<String, Object>> m_colors = new ArrayList<>();
    // Running IDs for creating new ones
    int m_colorId = 0;
    int m_userId = 0;

    public ColorServer() throws IOException {
        m_server = HttpServer.create(new InetSocketAddress("127.0.0.1", 49426), 0);
        m_server.createContext("/api/unknown", this::handleUnknown);
        m_server.createContext("/api/users", this::handleUsers);
        m_server.createContext("/api/login", this::handleLogin);
        m_server.createContext("/api/logout", this::handleLogout);
        populateColorsAndUsers();
    }

    public void start() throws IOException {
        System.out.println("Starting Bundled Java Server at :" + m_server.getAddress());
        m_server.start();
    }

    public void stop() {
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

    private int generateColourId() {
        return ++m_colorId;
    }

    private void addColor(String name, int year, String color, String pantone) {
        Map<String, Object> newColor = new HashMap<>();
        newColor.put("id", generateColourId());
        newColor.put("name", name);
        newColor.put("pantone_value", pantone);
        newColor.put("color", color);
        newColor.put("year", year);
        m_colors.add(newColor);
    }

    private int generateUserId() {
        return ++m_userId;
    }

    private void addUser(String email, String firstName, String lastName, String avatar) {
        Map<String, Object> newUser = new HashMap<>();
        newUser.put("id", generateUserId());
        newUser.put("first_name", firstName);
        newUser.put("last_name", lastName);
        newUser.put("email", email);
        newUser.put("avatar", avatar);
        m_users.add(newUser);
    }

    private void populateColorsAndUsers() {
        // Copied from QtHttpServer colorpalette example server
        addUser("rest.native@qt.io", "Rest", "Native", "users/1-user.jpg");
        addUser("chief.architect@qt.io", "Chief", "Architect", "users/2-user.jpg");
        addUser("rising.star@qt.io", "Rising", "Star", "users/3-user.jpg");
        addUser("creative.wizard@qt.io", "Creative", "Wizard", "users/4-user.jpg");
        addUser("knuth.developer@qt.io", "Knuth", "Developer", "users/5-user.jpg");
        addUser("white.hathacker@qt.io", "White", "Hathacker", "users/6-user.jpg");
        addUser("kind.programmer@qt.io", "Kind", "Programmer", "users/7-user.jpg");
        addUser("server.monk@qt.io", "Server", "Monk", "users/8-user.jpg");
        addUser("fast.fingers@qt.io", "Fast", "Fingers", "users/9-user.jpg");
        addUser("runtime.lord@qt.io", "Runtime", "Lord", "users/10-user.jpg");
        addUser("stack.flow@qt.io", "Stack", "Flow", "users/11-user.jpg");
        addUser("cute.coder@qt.io", "Cute", "Coder", "users/12-user.jpg");

        addColor("cerulean", 2000, "#98B2D1", "15-4020");
        addColor("fuchsia rose", 2001, "#C74375", "17-2031");
        addColor("true red", 2002, "#BF1932", "19-1664");
        addColor("aqua sky", 2003, "#7BC4C4", "14-4811");
        addColor("tigerlily", 2004, "#E2583E", "17-1456");
        addColor("blue turquoise", 2005, "#53B0AE", "15-5217");
        addColor("sand dollar", 2006, "#DECDBE", "13-1106");
        addColor("chili pepper", 2007, "#9B1B30", "19-1557");
        addColor("blue iris", 2008, "#5A5B9F", "18-3943");
        addColor("mimosa", 2009, "#F0C05A", "14-0848");
        addColor("turquoise", 2010, "#45B5AA", "15-5519");
        addColor("honeysuckle", 2011, "#D94F70", "18-2120");
    }
}
