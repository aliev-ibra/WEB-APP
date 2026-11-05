# Lab 2.1 - Task Manager REST API

A RESTful task-management application built on the provided minimal, socket-based Java HTTP framework. The framework parses raw HTTP from a `ServerSocket`; this project adds a resource (`Task`), a REST controller with full CRUD, and a single-page HTML frontend. It is the baseline that Lab 2.2 (cookies, see [lab3-README.md](lab3-README.md)) builds on.

## Structure

```
src/main/java/com/vizja/swp/lab2/
|-- Application.java            entry point: registers routes, seeds sample tasks, starts the server on 8080
|-- controller/
|   |-- HomeController.java     GET /      -> single-page HTML/CSS/JS frontend
|   `-- TasksController.java    /tasks     -> REST CRUD (JSON)
|-- model/Task.java             entity: id, title, description, status (TODO | IN_PROGRESS | DONE), createdAt
|-- repository/TaskRepository.java   thread-safe in-memory store (ConcurrentHashMap)
`-- lib/                        PROVIDED FRAMEWORK - not modified (Server, FrontController, BaseController, http/*)
```

Application code is everything outside `lib/`. It only uses the framework's public API (`BaseController`, `FrontController.addRoute`, `HttpRequest`, `HttpResponse`).

## REST API

Base URL `http://localhost:8080`. Responses use `application/json; charset=UTF-8`.

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| `GET` | `/tasks` | none | `200` + JSON array | none |
| `POST` | `/tasks` | `{"title","description?","status?"}` | `201 Created` + the new task | `400` blank title, bad status, invalid JSON |
| `PUT` | `/tasks` | `{"id","title?","description?","status?"}` | `200` + the updated task | `400` bad input or missing id, `404` unknown id |
| `DELETE` | `/tasks` | `{"id"}` | `204 No Content` | `400` missing id, `404` unknown id |

The framework routes exact paths only, so the task `id` is sent in the JSON body for `PUT` and `DELETE` instead of in the URL. Other HTTP methods on `/tasks` return `405`. In this version an unknown path ends in `500` (the Lab 2.1 `FrontController` has no "no route" handling, and it is framework code that must not be changed).

```bash
curl http://localhost:8080/tasks
curl -X POST   http://localhost:8080/tasks -H "Content-Type: application/json" -d '{"title":"Review lab","status":"TODO"}'
curl -X PUT    http://localhost:8080/tasks -H "Content-Type: application/json" -d '{"id":5,"status":"DONE"}'
curl -X DELETE http://localhost:8080/tasks -H "Content-Type: application/json" -d '{"id":5}'
```

## Frontend

Served at `http://localhost:8080/`. Task cards with status filters (All / To Do / In Progress / Done), live counters, an add/edit modal (pre-filled when editing), delete with confirmation, and toast messages. All requests go through `fetch()` to the REST API, and user-supplied text is HTML-escaped (`escHtml`) before it is rendered.

## Running

From the repository root:

```bash
./gradlew runLab2     # Windows: gradlew.bat runLab2
```

or run `com.vizja.swp.lab2.Application` from your IDE. Requires JDK 17-21 (the bundled Gradle 8.14 supports up to Java 24). Lab 2.2 shares port 8080, so run only one lab at a time.

**Dependencies** (from `build.gradle`): SLF4J API and Simple 2.0.17, and Jackson Databind 3.0.0 (Java package `tools.jackson`; this is a valid, released artifact).

## Submission

`task-manager-app.zip` (in `submissions/`) contains only the application code, laid out like this:

```
src/main/java/com/vizja/swp/lab2/{Application.java, controller/*, model/*, repository/*}
src/main/resources/simplelogger.properties
```

The `lib/` framework, Gradle files and build output are not included.

## Design notes

- **Thread safety:** `TaskRepository` uses a `ConcurrentHashMap`; the framework serves requests from a fixed pool of three threads.
- **Validation:** the title is required and non-blank, the status must be a valid enum value, and the id must be a positive number for `PUT` and `DELETE`.
- **Errors:** controllers return `200`, `201`, `204`, `400`, `404` or `500` with a JSON `{"error": "..."}` body.

## Known gaps (baseline for later security labs)

No authentication or authorisation, predictable sequential ids (IDOR risk), no CSRF protection, in-memory storage, verbose error messages, and HTTP only (no TLS).
