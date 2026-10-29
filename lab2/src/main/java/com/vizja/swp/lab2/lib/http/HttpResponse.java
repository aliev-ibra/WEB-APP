package com.vizja.swp.lab2.lib.http;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {
    private final StringWriter bodyWriter = new StringWriter();
    private final PrintWriter writer = new PrintWriter(bodyWriter);
    private final Map<String, String> headers = new LinkedHashMap<>();
    private int statusCode = 200;
    private String statusMessage = "OK";

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

        headers.putIfAbsent("Content-Type", "text/html; charset=UTF-8");
        int contentLength = body.getBytes(StandardCharsets.UTF_8).length;
        headers.putIfAbsent("Content-Length", String.valueOf(contentLength));

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");
        headers.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\r\n"));
        sb.append("\r\n");
        sb.append(body);
        return sb.toString();
    }
}
