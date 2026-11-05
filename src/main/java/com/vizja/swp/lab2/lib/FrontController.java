package com.vizja.swp.lab2.lib;



import com.vizja.swp.lab2.lib.http.HttpRequest;
import com.vizja.swp.lab2.lib.http.HttpResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FrontController {
    private static final Map<String, BaseController> routes = new ConcurrentHashMap<>();

    public static HttpResponse handle(HttpRequest request) {
        HttpResponse response = new HttpResponse();

        var controller = routes.get(request.getPath());

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


    public static Map<String, BaseController> addRoute(String path, BaseController controller) {
        routes.put(path, controller);
        return routes;
    }
}
