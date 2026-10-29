package com.vizja.swp.lab2.lib.http;

public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD;

    public static HttpMethod fromString(String method) {
        try {
            return HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
