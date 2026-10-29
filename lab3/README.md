# Lab 2.2 - Task Manager with HTTP Cookie State Management

A task-management web application running on a minimal, socket-based Java HTTP server. This lab continues the Lab 2.1 REST application (`../lab2`) and adds **cookie-based state management**: the server issues cookies, and the page's JavaScript reads and writes them to change what the user sees.

> HTTP is stateless. Cookies are how a stateless protocol "remembers" a browser between requests. This project shows the complete cookie round trip on top of hand-parsed HTTP.

## Contents

1. [What this lab adds](#what-this-lab-adds)
2. [Features](#features)
3. [Project structure](#project-structure)
4. [How a request flows through the code](#how-a-request-flows-through-the-code)
5. [REST API](#rest-api)
6. [Cookies in detail](#cookies-in-detail)
7. [Client-side JavaScript](#client-side-javascript)
8. [Running the project](#running-the-project)
9. [Trying it out](#trying-it-out)
10. [Testing and verification](#testing-and-verification)
11. [Security notes and known limitations](#security-notes-and-known-limitations)
12. [Requirements checklist](#requirements-checklist)

## What this lab adds

| Requirement | Where it is implemented |
|---|---|
| Server creates and transmits an HTTP cookie | `HomeController.doGet` calls `response.addCookie(...)`; the framework writes one `Set-Cookie:` header per cookie |
| Client-side JavaScript reads the server's cookie | `getCookie('visits')`, `getCookie('prevVisit')` in the page script (`document.cookie`) |
| Cookie data visibly changes the user experience | A personalised welcome banner ("First visit" vs "Welcome back, visit #N, last visit X ago") |
| Client-side cookie manipulation | A `theme` cookie written by JavaScript; a "Reset visitor data" button that deletes cookies |
| Integrates cleanly with the existing CRUD | The REST API and UI from Lab 2.1 are unchanged and keep working with cookies present |
| Framework untouched | All cookie logic lives in the application layer (`controller/`); `lib/` is byte-identical to the provided framework |

## Features

- **Task CRUD** over a REST API (`/tasks`): create, list, update, delete, with input validation and meaningful status codes.
- **Single-page frontend** served by the server itself (HTML, CSS and JavaScript embedded in `HomeController`): task cards, status filters, live counters, add/edit modal, delete with confirmation, toast notifications.
- **Visit tracking** with server-set cookies and a welcome banner.
- **Dark / light theme** remembered in a client-set cookie.
- **No external frontend dependencies** - plain HTML, CSS and JavaScript.

## Project structure

```
lab3/
|-- build.gradle                 provided (unmodified)
|-- settings.gradle              provided
|-- gradlew, gradlew.bat, gradle/wrapper/   Gradle 8.14 wrapper
`-- src/main/
    |-- java/com/vizja/lab3/
    |   |-- App.java                       entry point: registers routes, seeds data, starts the server
    |   |-- controller/
    |   |   |-- HomeController.java        GET /  -> HTML page + issues the visit cookies
    |   |   `-- TasksController.java       /tasks -> REST CRUD (JSON)
    |   |-- model/Task.java                task entity + status enum
    |   |-- repository/TaskRepository.java thread-safe in-memory store
    |   `-- lib/                           PROVIDED FRAMEWORK - not modified
    |       |-- Server.java                ServerSocket accept loop + thread pool
    |       |-- FrontController.java       exact-path router (404 for unknown paths)
    |       |-- BaseController.java        dispatches GET/POST/PUT/DELETE to doXxx()
    |       `-- http/                      HttpRequest, HttpResponse, Cookie, Session, SessionManager, ...
    `-- resources/simplelogger.properties  SLF4J logging to stdout
```

Application code is everything outside `lib/`. The application only depends on the framework's public API, so the two layers stay decoupled.

## How a request flows through the code

```
Browser
  | raw HTTP over a TCP socket
  v
Server (ServerSocket.accept, 3-thread pool)
  | HttpRequest.parse(): request line, headers, Cookie header -> cookie map, body (Content-Length)
  v
FrontController.handle()   exact path match: "/" -> HomeController, "/tasks" -> TasksController
  v
BaseController.handle()    switch on HTTP method -> doGet / doPost / doPut / doDelete
  v
Controller (application code)
  | fills HttpResponse: status, headers, body, cookies
  v
HttpResponse.toString()    status line + headers + one "Set-Cookie:" line per cookie + blank line + body
  v
Browser
```

The framework matches **exact paths** only, so a path like `/tasks/5` is not routable. That is why the task `id` travels in the JSON body for `PUT` and `DELETE`.

## REST API

Base URL: `http://localhost:8080`. All `/tasks` responses are `application/json; charset=UTF-8`.

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| `GET` | `/tasks` | - | `200` + JSON array of tasks | - |
| `POST` | `/tasks` | `{"title":"...","description":"...","status":"TODO"}` | `201 Created` + the new task | `400` blank title, bad status, invalid JSON |
| `PUT` | `/tasks` | `{"id":1,"title":"...","description":"...","status":"DONE"}` (fields other than `id` optional) | `200` + the updated task | `400` bad input / missing id, `404` unknown id |
| `DELETE` | `/tasks` | `{"id":1}` | `204 No Content` | `400` missing id, `404` unknown id |
| any other | `/tasks` | - | - | `405 Method Not Allowed` |

A task looks like:

```json
{ "id": 1, "title": "Write tests", "description": "...", "status": "IN_PROGRESS", "createdAt": "2026-10-29T18:39:32.748" }
```

`status` is one of `TODO`, `IN_PROGRESS`, `DONE` (case-insensitive on input). Unknown paths return `404`. Errors are returned as `{"error":"..."}`.

## Cookies in detail

### Cookies set by the server

Every `GET /` response carries three `Set-Cookie` headers (built in `HomeController`):

| Cookie | Value | Purpose | Attributes |
|---|---|---|---|
| `visits` | number of page loads by this browser, including the current one | drives "visit #N" | `Max-Age=31536000; Path=/; SameSite=Lax` |
| `lastVisit` | epoch milliseconds of this load | becomes the "previous visit" on the next load | `Max-Age=31536000; Path=/; SameSite=Lax` |
| `prevVisit` | epoch milliseconds of the *previous* load | lets the page say "last visit X ago" even though `lastVisit` was just overwritten | `Max-Age=31536000; Path=/; SameSite=Lax` (sent with `Max-Age=0` to delete it on a first visit) |

Example response headers on a second visit:

```
HTTP/1.1 200 OK
Content-Type: text/html; charset=UTF-8
Content-Length: ...
Set-Cookie:visits=2; Max-Age=31536000; Path=/; SameSite=Lax
Set-Cookie:lastVisit=1793295667719; Max-Age=31536000; Path=/; SameSite=Lax
Set-Cookie:prevVisit=1793295661638; Max-Age=31536000; Path=/; SameSite=Lax
```

### Cookie set by the client

| Cookie | Set by | Value | Purpose |
|---|---|---|---|
| `theme` | JavaScript (`toggleTheme()`) | `dark` or `light` | remembers the colour theme across reloads |

### Lifecycle

1. **First visit** - the browser sends no `Cookie` header. The server sets `visits=1` and `lastVisit`. The page shows "Welcome! First visit".
2. **Return visit** - the browser automatically sends `Cookie: visits=1; lastVisit=...`. `HttpRequest.parse` turns the header into a cookie map; `HomeController` reads it, increments the counter and re-issues the cookies. The page shows "Welcome back! This is visit #2".
3. **Client-side change** - clicking *Dark mode* writes `theme=dark` with `document.cookie`. On the next page load the script reads it and applies the theme before the user interacts.
4. **Expiry / deletion** - cookies live for one year (`Max-Age`). *Reset visitor data* deletes them by setting `Max-Age=0`.

### Why the cookies are not `HttpOnly`

The lab requires JavaScript to **read** the server's cookie, and `HttpOnly` hides a cookie from `document.cookie`. These cookies are therefore deliberately readable by scripts. They hold only harmless counters and timestamps, never credentials or session identifiers. A real session ID would be `HttpOnly` (and `Secure` over HTTPS).

### Treating cookies as untrusted input

A client can send any value in a `Cookie` header. `HomeController` never trusts them: values are parsed defensively (`Long.parseLong` inside a try/catch), negative or non-numeric values become `0`, and the visit counter is capped at 1,000,000. For example `visits=abc` is reset to `1`, and `visits=99999999999` is clamped.

## Client-side JavaScript

The script is embedded in `HomeController` and organised in small functions:

| Function | Role |
|---|---|
| `getCookie(name)` | parses `document.cookie` and returns one cookie's decoded value |
| `setCookie(name, value, days)` | writes a cookie with `Max-Age`, `Path=/`, `SameSite=Lax` |
| `deleteCookie(name)` | expires a cookie (`Max-Age=0`) |
| `initCookies()` | applies the saved theme and builds the welcome banner from `visits` / `prevVisit` |
| `toggleTheme()` / `applyTheme()` | switch and persist the dark / light theme |
| `resetVisitorData()` | deletes `visits`, `lastVisit`, `prevVisit`, `theme` |
| `api(method, path, body)` | `fetch` wrapper used by all CRUD calls |
| `loadTasks()`, `render()`, `submitForm()`, `editTask()`, `deleteTask()` | the task UI |

All user-supplied text (titles and descriptions) is HTML-escaped before it is inserted into the page (`escHtml`).

## Running the project

**Requirements:** JDK 17-21 (Gradle 8.14, bundled as the wrapper, supports up to Java 24) and internet access for the first dependency download.

The provided `build.gradle` has no `run` task, so there are two options:

1. **From an IDE (recommended):** open the `lab3` folder as a Gradle project and run the `com.vizja.lab3.App` class.
2. **Compile with Gradle:**

   ```bash
   ./gradlew build        # Windows: gradlew.bat build
   ```

   then run `com.vizja.lab3.App` from your IDE, or start it with `java -cp` using the compiled classes plus the dependencies (`slf4j-api`, `slf4j-simple`, `jackson-databind` 3.0.0, `jackson-core`, `jackson-annotations`).

Then open <http://localhost:8080>. The server listens on port `8080` (set in `App.java`). Data is kept in memory and seeded with four sample tasks on every start.

**Dependencies** (from `build.gradle`): SLF4J API and Simple 2.0.17, Jackson Databind 3.0.0 (package `tools.jackson`), and Lombok via the `io.freefair.lombok` plugin (used by the provided framework).

## Trying it out

**In the browser**

1. Open <http://localhost:8080> - you see "Welcome! First visit".
2. Reload - "Welcome back! This is visit #2", with the time of your previous visit.
3. Click **Dark mode**, reload - the theme persists.
4. Open the developer tools (Application / Storage -> Cookies) to see `visits`, `lastVisit`, `prevVisit` and `theme`.
5. Add, edit and delete a task - the cookies are unaffected.
6. Click **Reset visitor data** and reload to start fresh.

**With curl** (shows the raw cookie round trip)

```bash
# first visit: no cookie sent, three Set-Cookie headers come back
curl -i -c jar.txt http://localhost:8080/

# second visit: send the saved cookies back, visits becomes 2
curl -i -b jar.txt -c jar.txt http://localhost:8080/

# CRUD
curl -i http://localhost:8080/tasks
curl -i -X POST   http://localhost:8080/tasks -H "Content-Type: application/json" -d '{"title":"Try cookies","status":"TODO"}'
curl -i -X PUT    http://localhost:8080/tasks -H "Content-Type: application/json" -d '{"id":5,"status":"DONE"}'
curl -i -X DELETE http://localhost:8080/tasks -H "Content-Type: application/json" -d '{"id":5}'
```

## Testing and verification

The project has no automated unit tests (the provided build disables them). It was verified manually against a running server, built and run through Gradle (JDK 21), with raw HTTP requests and in a real browser:

- **REST:** create returns 201; list shows the new task; update returns 200 and persists; delete returns 204, the task disappears, and deleting again returns 404; blank titles, bad statuses and malformed JSON return 400; unknown HTTP methods return 405; unknown paths return 404.
- **Cookies (server):** a first visit receives the `Set-Cookie` headers; sending `visits=1` back yields `visits=2` and a `prevVisit`; tampered values (`visits=abc`, negative numbers, huge numbers) are sanitised.
- **Cookies (browser):** visit 1 shows the first-visit banner; reloading shows "visit #2"; the dark theme set by JavaScript survives a reload; *Reset visitor data* empties the cookie jar.
- **UI CRUD:** adding, editing and deleting a task through the real form and buttons works, and cookies are unchanged afterwards.

## Security notes and known limitations

This codebase is the baseline for later security labs, so these are deliberate or acknowledged gaps rather than oversights:

- **No authentication or authorisation** - anyone who can reach the port can read and change every task.
- **Cookies are not `Secure`** because the app runs over plain HTTP on localhost; over HTTPS they should be `Secure`.
- **No CSRF protection** on the state-changing endpoints (`SameSite=Lax` on the cookies helps only for cookies, and the API does not use cookie-based auth).
- **In-memory storage** - data is lost on restart and the id counter is a static `AtomicLong`.
- **Task id in the request body** instead of the URL, forced by the framework's exact-path routing.
- **`DELETE` with a body** is unusual (allowed by HTTP, but some proxies drop it).
- **Request parsing is basic** (framework): no chunked bodies, no keep-alive, no header size limits, one request per connection.
- **Fixed thread pool of 3** (framework), so a few slow clients can starve the server.
- The framework also provides `Session` / `SessionManager` (server-side sessions identified by a cookie). This lab only needs plain cookies, so sessions are not used here.

## Requirements checklist

- [x] Built on the provided socket-based framework, which is unmodified
- [x] Continues the Lab 2.1 application (same task domain and CRUD)
- [x] Server-side cookie generation via `Set-Cookie` response headers
- [x] Client-side JavaScript reads the server-set cookie
- [x] Cookie data changes the user experience (personalised welcome, remembered theme)
- [x] Cookie handling is entirely in the application layer
- [x] Cookie features coexist with the existing CRUD operations
