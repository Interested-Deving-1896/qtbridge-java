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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.net.URIBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Simple single-threaded HTTP server for colorpaletteclient.
// There are no long or I/O bound operations and this should suffice for
// the example's purposes (hence no concurrency protection either). Furthermore
// the server implementation assumes well-behaving client (this same app) and
// does not do much in terms of validating the requests.
public class ColorServer {
    private final HttpServer m_server;
    private final ObjectMapper m_jsonMapper;
    private final int ITEMS_PER_PAGE = 6;
    private boolean m_loggedIn = false;
    // Resource (users and colors) containers
    private final List<Map<String, Object>> m_users = new ArrayList<>();
    private final List<Map<String, Object>> m_colors = new ArrayList<>();
    // Running IDs for creating new ones
    int m_colorId = 0;
    int m_userId = 0;

    public ColorServer() throws IOException {
        m_server = HttpServer.create(new InetSocketAddress("127.0.0.1", 49426), 0);
        m_jsonMapper = new ObjectMapper();
        m_server.createContext("/api/colors", this::handleColors);
        m_server.createContext("/api/users", this::handleUsers);
        m_server.createContext("/api/login", this::handleLogin);
        m_server.createContext("/api/logout", this::handleLogout);
        populateColorsAndUsers();
    }

    public void start() throws IOException {
        m_server.start();
    }

    public void stop() {
        m_server.stop(0);
    }

    private void handleColors(HttpExchange exchange) throws IOException {
        String requestHttpMethod = exchange.getRequestMethod();
        if (requestHttpMethod.equals("GET")) {
            // Get a pageful of colors
            handlePagedGet(exchange, m_colors);
            return;
        } else if (requestHttpMethod.equals("DELETE")) {
            // Delete a color
            handleColorDelete(exchange);
        } else if (requestHttpMethod.equals("POST")) {
            // Add a new color
            handleColorAdd(exchange);
        } else if (requestHttpMethod.equals("PUT")) {
            // Update an existing color
            handleColorUpdate(exchange);
        }
        respondError(exchange, 405); // Method not allowed
    }

    private void handleColorUpdate(HttpExchange exchange) throws IOException {
        if (!m_loggedIn) {
            respondError(exchange, 401);
            return;
        }
        Map<String, Object> requestColor =
            m_jsonMapper.readValue(exchange.getRequestBody(), Map.class);

        // Get the color 'id' from path
        Integer id = null;
        String[] components = exchange.getRequestURI().getPath().split("/");
        boolean success = false;
        if (components.length == 4) // "", "api", "colors", "<id>"
            id = Integer.parseInt(components[3]);

        if (id == null) {
            respondError(exchange, 404); // Not found
            return;
        }

        // Find and update the requested color
        for (Map<String, Object> color : m_colors) {
            if (!id.equals(color.get("id")))
                continue;
            color.put("name", (String) requestColor.getOrDefault("name", "<empty>"));
            color.put("color", (String) requestColor.getOrDefault("color", "<empty>"));
            color.put("pantone_value", (String) requestColor.getOrDefault("pantone_value", "<empty>"));
            // Color update OK. The client does not parse response data -> don't send either
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        respondError(exchange, 404); // Not found
    }

    private void handleColorAdd(HttpExchange exchange) throws IOException {
        if (!m_loggedIn) {
            respondError(exchange, 401); // Not authorized
            return;
        }
        Map<String, Object> requestColor =
            m_jsonMapper.readValue(exchange.getRequestBody(), Map.class);

        String name = (String) requestColor.get("name");
        String color = (String) requestColor.get("color");
        String pantone = (String) requestColor.get("pantone_value");

        if (name == null || color == null || pantone == null) {
            respondError(exchange, 400); // Bad request
            return;
        }

        // Add the new color to colorlist
        Map<String, Object> newColor = new HashMap<>();
        newColor.put("id", generateColourId());
        newColor.put("name", name);
        newColor.put("pantone_value", pantone);
        newColor.put("color", color);
        m_colors.add(newColor);

        // Color addition OK. The client does not parse response data -> don't send either
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private void handleColorDelete(HttpExchange exchange) throws IOException {
        if (!m_loggedIn) {
            respondError(exchange, 401); // Not authorized
            return;
        }

        // Get the color id from path and remove corresponding color
        String[] components = exchange.getRequestURI().getPath().split("/");
        boolean success = false;
        if (components.length == 4) { // "", "api", "colors", "<id>"
            int id = Integer.parseInt(components[3]);
            // Remove the color with matching ID
            success = m_colors.removeIf(color -> id == (Integer)color.get("id"));
        }
        if (success) {
            exchange.sendResponseHeaders(204, -1); // No content (success)
            exchange.close();
            return;
        }
        respondError(exchange, 404); // Not found
    }

    private void handleUsers(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("GET")) {
            handlePagedGet(exchange, m_users);
            return;
        }
        respondError(exchange, 405); // Method not allowed
        return;
    }

    // Handle GET requests for users and colors
    private void handlePagedGet(HttpExchange exchange, List<Map<String, Object>> resource) throws IOException {
        // Get requested 'page' from URI query parameters
        URIBuilder requestUri = new URIBuilder(exchange.getRequestURI());
        int requestedPage = requestUri.getFirstQueryParam("page") != null
                            ? Integer.parseInt(requestUri.getFirstQueryParam("page").getValue()) : 1;

        final int totalItems = resource.size();
        // Number of pages we're able to serve in total
        final int totalPages = Math.max(1, (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);

        // If the requested page is too high (not enough items), return highest possible
        requestedPage = Math.min(Math.max(1, requestedPage), totalPages);

        // Take the requested page as a sublist
        int fromItem = Math.max(0, (requestedPage - 1) * ITEMS_PER_PAGE);
        int toItem = Math.min(fromItem + ITEMS_PER_PAGE, totalItems);
        List<Map<String, Object>> responseData = resource.subList(fromItem, toItem);

        // Create the response data
        Map<String, Object> response = new HashMap<>();
        response.put("page", requestedPage);
        response.put("per_page", ITEMS_PER_PAGE);
        response.put("total", totalItems);
        // Total pages is at minimum '1' even when no items
        response.put("total_pages", totalPages);
        response.put("data", responseData);

        // Convert response data to JSON
        byte[] responseJson = m_jsonMapper.writeValueAsBytes(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseJson.length);
        exchange.getResponseBody().write(responseJson);
        exchange.close();
    }

    private void respondError(HttpExchange exchange, int httpStatus) throws IOException {
        exchange.sendResponseHeaders(httpStatus, -1); // 405: Method Not Allowed
        exchange.close();
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("POST")) {
            // Record whether or not someone is currently logged in or not.
            // Needless to say this is not real login handling, but something
            // that suffices for what the colorpalette app actually demonstrates
            // (Qt Java Bridging).
            m_loggedIn = true;
            byte[] responseJson = m_jsonMapper.writeValueAsBytes(Map.of("token", "secret_token"));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseJson.length);
            exchange.getResponseBody().write(responseJson);
            exchange.close();
            return;
        }
        respondError(exchange, 405); // Method not allowed
        return;
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("POST")) {
            // Record whether or not someone is currently logged in or not.
            // Needless to say this is not real login handling, but something
            // that suffices for what the colorpalette app actually demonstrates
            // (Qt Java Bridging).
            m_loggedIn = false;
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        respondError(exchange, 405); // Method not allowed
        return;
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
