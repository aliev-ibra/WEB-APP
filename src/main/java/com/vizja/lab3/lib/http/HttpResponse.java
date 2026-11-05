package com.vizja.lab3.lib.http;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vizja.lab3.lib.http.HttpUtil.CRLF;
import static com.vizja.lab3.lib.http.HttpUtil.HEADER_CONTENT_LENGTH;
import static com.vizja.lab3.lib.http.HttpUtil.HEADER_CONTENT_TYPE;
import static com.vizja.lab3.lib.http.HttpUtil.HEADER_SET_COOKIE;
import static com.vizja.lab3.lib.http.HttpUtil.SUPPORTED_HTTP_VERSIONS;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class HttpResponse {
    private final StringWriter bodyWriter = new StringWriter();
    private final PrintWriter writer = new PrintWriter(bodyWriter);
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final List<Cookie> cookies = new ArrayList<>(); // Added to support cookies
    private int statusCode = 200;
    private String statusMessage = "OK";

    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public void setStatus(int code, String message) {
        this.statusCode = code;
        this.statusMessage = message;
    }

    public void setHeader(String key, String value) {
        headers.put(key, value);
    }


    @Override
    public String toString() {
        writer.flush();
        String body = bodyWriter.toString();

        // Set default headers if they haven't been set manually
        headers.putIfAbsent(HEADER_CONTENT_TYPE, "text/html; charset=UTF-8");
        int contentLength = body.getBytes(UTF_8).length;
        headers.putIfAbsent(HEADER_CONTENT_LENGTH, String.valueOf(contentLength));

        final var responseBuilder = new StringBuilder();

        // 1. Status Line
        responseBuilder.append(SUPPORTED_HTTP_VERSIONS).append(" ")
                .append(statusCode).append(" ")
                .append(statusMessage)
                .append(CRLF);

        // 2. Standard Headers
        final String headersString = headers.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue() + CRLF)
                .collect(Collectors.joining());
        responseBuilder.append(headersString);

        // 3. Cookie Headers
        // Each cookie must have its own "Set-Cookie:" header line.
        final String cookiesString = cookies.stream()
                .map(cookie -> HEADER_SET_COOKIE + ":" + cookie.toString() + CRLF)
                .collect(Collectors.joining());
        responseBuilder.append(cookiesString);

        // 4. Separator between headers and body
        responseBuilder.append(CRLF);

        // 5. Body
        responseBuilder.append(body);

        return responseBuilder.toString();
    }
}
