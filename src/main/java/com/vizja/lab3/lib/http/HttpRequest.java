package com.vizja.lab3.lib.http;

import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.vizja.lab3.lib.http.HttpUtil.HEADER_CONTENT_LENGTH;
import static com.vizja.lab3.lib.http.HttpUtil.HEADER_COOKIE;

public class HttpRequest {
    @Getter
    private final HttpMethod method;
    @Getter
    private final String path;
    @Getter
    private final String version;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, Cookie> cookies = new HashMap<>();
    @Getter
    private String body;
    @Getter
    @Setter
    private Session session;

    public HttpRequest(HttpMethod method, String path, String version) {
        this.method = method;
        this.path = path;
        this.version = version;
    }

    public static HttpRequest parse(String requestLine, BufferedReader reader) throws IOException {
        String[] parts = requestLine.split(" ");
        var method = HttpMethod.fromString(parts[0]);
        var path = parts[1];
        var version = parts[2];

        var request = new HttpRequest(method, path, version);

        // Parse Headers
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int idx = line.indexOf(":");
            if (idx > 0) {
                var key = line.substring(0, idx).trim();
                var value = line.substring(idx + 1).trim();
                request.headers.put(key, value);
            }
        }

        // --- ADDED: PARSE COOKIES FROM HEADER ---
        if (request.headers.containsKey(HEADER_COOKIE)) {
            String cookieHeader = request.headers.get(HEADER_COOKIE);
            String[] cookiePairs = cookieHeader.split(";\\s*");
            for (String pair : cookiePairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    String name = pair.substring(0, idx).trim();
                    String value = pair.substring(idx + 1).trim();
                    request.cookies.put(name, new Cookie(name, value));
                }
            }
        }
        // ------------------------------------------

        // Parse Body
        if (request.headers.containsKey(HEADER_CONTENT_LENGTH)) {
            int length = Integer.parseInt(request.headers.get(HEADER_CONTENT_LENGTH));
            char[] buf = new char[length];
            reader.read(buf, 0, length);
            request.body = new String(buf);
        }

        return request;
    }


    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public Optional<String> getHeader(String name) {
        return Optional.ofNullable(headers.get(name));
    }

    // --- ADDED: COOKIE GETTERS ---
    public Map<String, Cookie> getCookies() {
        return Collections.unmodifiableMap(cookies);
    }

    public Optional<Cookie> getCookie(String name) {
        return Optional.ofNullable(cookies.get(name));
    }
    // ----------------------------

}