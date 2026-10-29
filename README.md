# WEB-APP

Task Manager web application built on a minimal socket-based Java HTTP framework (course: Security of Web Applications).

| Folder | Lab | What it adds |
|---|---|---|
| `lab2/` | Lab 2.1 | RESTful CRUD API (`/tasks`) and an HTML/JS frontend on the provided framework |
| `lab3/` | Lab 2.2 | Everything from Lab 2.1, plus HTTP cookie state management (server-set `Set-Cookie`, client-side JavaScript reads and writes cookies) |

The framework code lives in each project's `lib/` package and is unmodified. Application code is in the `controller/`, `model/` and `repository/` packages plus `Application.java` / `App.java`.

## API (`/tasks`)

| Method | Purpose | Body |
|---|---|---|
| `GET` | List tasks | none |
| `POST` | Create a task (201) | `{"title","description?","status?"}` |
| `PUT` | Update a task | `{"id","title?","description?","status?"}` |
| `DELETE` | Delete a task (204) | `{"id"}` |

The framework matches exact paths only, so the task `id` travels in the JSON body rather than in the URL.

## Cookies (Lab 2.2)

On every `GET /` the server sets `visits`, `lastVisit` and `prevVisit`. The page script reads them to show a welcome-back banner. A `theme` cookie is written by JavaScript to remember dark mode. The cookies are intentionally not `HttpOnly` so the script can read them; they hold no credentials, and incoming values are validated server-side.

## Run

```
cd lab3             # or lab2
./gradlew build     # compiles the project
```

The provided `build.gradle` has no `run` task, so start the app by running the `App` class (lab3) or `Application` class (lab2) from your IDE. Then open <http://localhost:8080>. Use JDK 17-21 with the bundled Gradle 8.14 wrapper.
