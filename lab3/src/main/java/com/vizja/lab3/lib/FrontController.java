package com.vizja.lab3.lib;

import com.vizja.lab3.lib.http.HttpRequest;
import com.vizja.lab3.lib.http.HttpResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FrontController {
    private static final Map<String, BaseController> routes = new ConcurrentHashMap<>();

    public static HttpResponse handle(final HttpRequest request) {
        final var response = new HttpResponse();

        BaseController controller = routes.get(request.getPath());

        if (controller == null) {
            response.setStatus(404, "Not Found");
            response.getWriter().println("404 Not Found: No route for " + request.getPath());
            return response;
        }

        try {
            controller.handle(request, response);
        } catch (UnsupportedOperationException e) {
            response.setStatus(405, "Method Not Allowed");
            response.getWriter().println(e.getMessage());
        } catch (Exception e) {
            response.setStatus(500, "Internal Server Error");
            response.getWriter().println("Server error: " + e.getMessage());
        }

        return response;
    }


    public static void addRoute(String path, BaseController controller) {
        routes.put(path, controller);
    }
}
