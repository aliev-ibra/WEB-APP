package com.vizja.swp.lab2.lib;

import com.vizja.swp.lab2.lib.http.HttpRequest;
import com.vizja.swp.lab2.lib.http.HttpResponse;

public abstract class BaseController {

    public void handle(HttpRequest request, HttpResponse response) {
        switch (request.getMethod()) {
            case GET -> doGet(request, response);
            case POST -> doPost(request, response);
            case PUT -> doPut(request, response);
            case DELETE -> doDelete(request, response);
            default -> {
                response.setStatus(405, "Method Not Allowed");
                response.getWriter().println("405 Method Not Allowed");
            }
        }
    }

    public void doGet(HttpRequest request, HttpResponse response) {
        throw new UnsupportedOperationException("GET method not implemented");
    }

    public void doPost(HttpRequest request, HttpResponse response) {
        throw new UnsupportedOperationException("POST method not implemented");
    }

    public void doPut(HttpRequest request, HttpResponse response) {
        throw new UnsupportedOperationException("PUT method not implemented");
    }

    public void doDelete(HttpRequest request, HttpResponse response) {
        throw new UnsupportedOperationException("DELETE method not implemented");
    }

}
