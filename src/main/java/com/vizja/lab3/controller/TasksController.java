package com.vizja.lab3.controller;

import com.vizja.lab3.lib.BaseController;
import com.vizja.lab3.lib.http.HttpRequest;
import com.vizja.lab3.lib.http.HttpResponse;
import com.vizja.lab3.model.Task;
import com.vizja.lab3.repository.TaskRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;

/**
 * REST controller for the /tasks resource.
 *
 * <ul>
 *   <li>GET    /tasks         – list all tasks</li>
 *   <li>POST   /tasks         – create a new task (JSON body: title, description?, status?)</li>
 *   <li>PUT    /tasks         – update an existing task (JSON body: id, title?, description?, status?)</li>
 *   <li>DELETE /tasks         – delete a task (JSON body: id)</li>
 * </ul>
 *
 * All responses use Content-Type: application/json.
 * The task id is always sent in the request body to remain compatible with the
 * framework's exact-path routing (path parameters are not supported).
 */
public class TasksController extends BaseController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TaskRepository repository;

    public TasksController(TaskRepository repository) {
        this.repository = repository;
    }

    // -----------------------------------------------------------------------
    // GET /tasks – return all tasks as a JSON array
    // -----------------------------------------------------------------------
    @Override
    public void doGet(HttpRequest request, HttpResponse response) {
        try {
            Collection<Task> tasks = repository.findAll();
            String json = MAPPER.writeValueAsString(tasks);
            sendJson(response, 200, "OK", json);
        } catch (Exception e) {
            sendError(response, 500, "Failed to retrieve tasks: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // POST /tasks – create a new task
    // Body: { "title": "...", "description": "...", "status": "TODO" }
    // -----------------------------------------------------------------------
    @Override
    public void doPost(HttpRequest request, HttpResponse response) {
        try {
            JsonNode body = parseBody(request, response);
            if (body == null) return;

            String title = requireString(body, "title", response);
            if (title == null) return;

            String description = body.has("description") ? body.get("description").asText("") : "";
            Task.Status status = parseStatus(body);

            Task task = repository.save(new Task(title, description, status));
            sendJson(response, 201, "Created", MAPPER.writeValueAsString(task));
        } catch (IllegalArgumentException e) {
            sendError(response, 400, e.getMessage());
        } catch (Exception e) {
            sendError(response, 500, "Failed to create task: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // PUT /tasks – update an existing task
    // Body: { "id": 1, "title": "...", "description": "...", "status": "DONE" }
    // -----------------------------------------------------------------------
    @Override
    public void doPut(HttpRequest request, HttpResponse response) {
        try {
            JsonNode body = parseBody(request, response);
            if (body == null) return;

            long id = requireId(body, response);
            if (id < 0) return;

            Task task = repository.findById(id).orElse(null);
            if (task == null) {
                sendError(response, 404, "Task with id " + id + " not found");
                return;
            }

            if (body.has("title")) task.setTitle(body.get("title").asText());
            if (body.has("description")) task.setDescription(body.get("description").asText(""));
            if (body.has("status")) task.setStatus(parseStatus(body));

            repository.save(task);
            sendJson(response, 200, "OK", MAPPER.writeValueAsString(task));
        } catch (IllegalArgumentException e) {
            sendError(response, 400, e.getMessage());
        } catch (Exception e) {
            sendError(response, 500, "Failed to update task: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // DELETE /tasks – remove a task
    // Body: { "id": 1 }
    // -----------------------------------------------------------------------
    @Override
    public void doDelete(HttpRequest request, HttpResponse response) {
        try {
            JsonNode body = parseBody(request, response);
            if (body == null) return;

            long id = requireId(body, response);
            if (id < 0) return;

            if (repository.delete(id)) {
                response.setStatus(204, "No Content");
            } else {
                sendError(response, 404, "Task with id " + id + " not found");
            }
        } catch (Exception e) {
            sendError(response, 500, "Failed to delete task: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private JsonNode parseBody(HttpRequest request, HttpResponse response) throws Exception {
        String raw = request.getBody();
        if (raw == null || raw.isBlank()) {
            sendError(response, 400, "Request body is required");
            return null;
        }
        try {
            JsonNode body = MAPPER.readTree(raw);
            if (body == null || !body.isObject()) {
                sendError(response, 400, "Request body must be a JSON object");
                return null;
            }
            return body;
        } catch (Exception e) {
            sendError(response, 400, "Request body must contain valid JSON");
            return null;
        }
    }

    private String requireString(JsonNode body, String field, HttpResponse response) {
        if (!body.has(field) || body.get(field).asText("").isBlank()) {
            sendError(response, 400, "'" + field + "' is required and must not be blank");
            return null;
        }
        return body.get(field).asText().trim();
    }

    private long requireId(JsonNode body, HttpResponse response) {
        if (!body.has("id")) {
            sendError(response, 400, "'id' is required");
            return -1;
        }
        long id = body.get("id").asLong(-1);
        if (id <= 0) {
            sendError(response, 400, "'id' must be a positive integer");
            return -1;
        }
        return id;
    }

    private Task.Status parseStatus(JsonNode body) {
        if (!body.has("status")) return Task.Status.TODO;
        String raw = body.get("status").asText("");
        try {
            return Task.Status.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid status '" + raw + "'. Must be one of: TODO, IN_PROGRESS, DONE");
        }
    }

    private void sendJson(HttpResponse response, int code, String message, String json) {
        response.setStatus(code, message);
        response.setHeader("Content-Type", "application/json; charset=UTF-8");
        response.getWriter().print(json);
    }

    private void sendError(HttpResponse response, int code, String message) {
        String statusMessage = switch (code) {
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            default -> "Internal Server Error";
        };
        response.setStatus(code, statusMessage);
        response.setHeader("Content-Type", "application/json; charset=UTF-8");
        response.getWriter().print("{\"error\":\"" + escapeJson(message) + "\"}");
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
