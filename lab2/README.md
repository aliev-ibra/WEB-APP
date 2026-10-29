# Task Manager REST API

A RESTful web application built from scratch on top of a minimalistic Java socket-based server framework. This project demonstrates low-level HTTP request parsing and response generation using raw Java `ServerSocket`/`Socket` APIs, without relying on high-level web frameworks (Spring Boot, Jakarta EE, etc.).

## Project Purpose

This application is a **Task Management System** that exposes a RESTful API for CRUD operations on a `Task` resource, served alongside a single-page HTML frontend. It was built as the baseline codebase for a web application security course, where subsequent labs will analyze and mitigate vulnerabilities in this code.

## Architecture

```
com.vizja.swp.lab2
├── Application.java          # Composition root; wires controllers, repository, and starts the server
├── controller/
│   ├── HomeController.java   # Serves the HTML frontend (GET /)
│   └── TasksController.java  # REST controller for /tasks (GET, POST, PUT, DELETE)
├── model/
│   └── Task.java             # Task entity (id, title, description, status, createdAt)
└── repository/
    └── TaskRepository.java   # Thread-safe in-memory store (ConcurrentHashMap)
```

The provided socket-based framework (`lib/` package: `Server`, `FrontController`, `BaseController`, `http/*`) is treated as an immutable dependency and is **not** part of this submission. The application layer is completely decoupled from the framework source.

## RESTful API Endpoints

All JSON endpoints are served at `http://localhost:8080/tasks`. Content-Type is `application/json`.

| Method | Endpoint | Description | Request Body | Success Response |
|--------|----------|-------------|--------------|------------------|
| `GET` | `/tasks` | List all tasks | — | `200 OK` + JSON array |
| `POST` | `/tasks` | Create a new task | `{ "title": "...", "description": "...", "status": "TODO" }` | `201 Created` + JSON task |
| `PUT` | `/tasks` | Update an existing task | `{ "id": 1, "title": "...", "description": "...", "status": "DONE" }` | `200 OK` + JSON task |
| `DELETE` | `/tasks` | Delete a task | `{ "id": 1 }` | `204 No Content` |

**Notes:**
- `id` is auto-generated on creation and must be supplied in the request body for `PUT` and `DELETE`.
- `status` must be one of: `TODO`, `IN_PROGRESS`, `DONE`.
- `title` is required and must not be blank.

### Example cURL Commands

```bash
# List all tasks
curl -X GET http://localhost:8080/tasks

# Create a new task
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Review lab submission","description":"Check README and zip structure","status":"TODO"}'

# Update a task (replace id with an existing task id)
curl -X PUT http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"id":1,"title":"Review lab submission","description":"Final review before deadline","status":"IN_PROGRESS"}'

# Delete a task
curl -X DELETE http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"id":1}'
```

## Frontend

A single-page HTML interface is served at `http://localhost:8080/`. It allows users to:

- View all tasks with status-based filtering (All / To Do / In Progress / Done)
- See summary statistics per status
- Create new tasks via a modal form
- Edit existing tasks (pre-populated modal)
- Delete tasks with confirmation

The frontend communicates with the REST API via `fetch()` and includes XSS protection (`escHtml`) when rendering user-supplied content.

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| SLF4J API | 2.0.17 | Logging facade |
| SLF4J Simple | 2.0.17 | Logging backend (stdout) |
| Jackson Databind | 2.x | JSON serialization/deserialization |

> **Note:** The `build.gradle` in the original project directory listed an invalid Jackson coordinate (`tools.jackson.core:jackson-databind:3.0.0`). Use `com.fasterxml.jackson.core:jackson-databind:2.17.2` (or another released 2.x version) instead.

## Installation & Build

### Prerequisites

- JDK 17 or later (JDK 21 recommended)
- Gradle 8.x (optional; a PowerShell bootstrap script is also provided)

### Option 1: Gradle (recommended)

From the project root (`lab2/`):

```bash
# Build
./gradlew build   # or: gradlew.bat build  on Windows

# Run
./gradlew run     # requires the 'application' plugin; otherwise run manually below
```

If `gradlew run` is not configured, compile and run manually:

```bash
./gradlew compileJava
java -cp build/classes/java/main:$(./gradlew -q printRuntimeClasspath | tr '\n' ':') \
  com.vizja.swp.lab2.Application
```

Then open `http://localhost:8080` in your browser.

### Option 2: PowerShell Bootstrap (`dev.ps1`)

The project includes `dev.ps1`, which downloads dependencies, compiles with `javac`, and starts the server — bypassing Gradle entirely:

```powershell
# From the lab2/ directory
.\dev.ps1
```

This script downloads Jackson and SLF4J JARs into `libs/`, compiles all `.java` sources into `build/dev-classes/`, and launches `com.vizja.swp.lab2.Application` on port 8080.

### Option 3: Manual Compilation

```bash
# Create output directory
mkdir build/classes

# Compile (adjust classpath to include Jackson + SLF4J JARs)
javac -cp "libs/*" -d build/classes src/main/java/com/vizja/swp/lab2/**/*.java

# Copy resources
cp -r src/main/resources/* build/classes/

# Run
java -cp "build/classes:libs/*" com.vizja.swp.lab2.Application
```

## Submission Structure

The submission `.zip` must contain **only application source code**, excluding the provided framework. The correct structure is:

```
task-manager-app.zip
├── README.md                                  <-- include this file
├── gradle.properties
├── src/main/resources/simplelogger.properties
└── src/main/java/com/vizja/swp/lab2/
    ├── Application.java
    ├── controller/
    │   ├── HomeController.java
    │   └── TasksController.java
    ├── model/
    │   └── Task.java
    └── repository/
        └── TaskRepository.java
```

**Do NOT include:**
- The `lib/` framework package (`Server.java`, `FrontController.java`, `BaseController.java`, `http/*`)
- `.git/` version-control metadata
- `.gradle/` cache directories
- `build/` output or `.class` files
- `gradlew`, `gradlew.bat`, `gradle/wrapper/`
- `build.gradle`, `settings.gradle` (unless you fix the invalid Jackson dependency coordinate)
- IDE metadata (`.idea/`, `.vscode/`, `*.iml`, etc.)

## Design Notes

- **Thread safety:** `TaskRepository` uses `ConcurrentHashMap` to safely handle concurrent HTTP requests (the server uses a fixed thread pool of 3).
- **Input validation:** `title` is required and non-blank; `status` is validated against the enum; `id` must be a positive integer for `PUT`/`DELETE`.
- **Error handling:** All endpoints return appropriate HTTP status codes (`200`, `201`, `204`, `400`, `404`, `500`) with JSON error bodies.
- **Framework immutability:** The application never modifies framework source files; it extends `BaseController` and registers routes via `FrontController.addRoute()`.

## Future Security Labs

This codebase is intended as the baseline for subsequent security-focused exercises. Potential areas for vulnerability analysis include:
- Input validation bypasses in the REST API
- IDOR (Insecure Direct Object Reference) via predictable sequential IDs
- Lack of authentication/authorization on all endpoints
- XSS if `escHtml` is bypassed or omitted
- Information disclosure via verbose error messages