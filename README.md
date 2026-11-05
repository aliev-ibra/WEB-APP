# labs

Course labs for Security of Web Applications, kept in one Gradle project. Every lab is a package under `src/main/java`, and each one builds on a minimal, socket-based Java HTTP framework (the `lib` package inside it, which is never modified).

| Lab | Package | What it is | Run |
|---|---|---|---|
| 2.1 | `com.vizja.swp.lab2` | Task Manager: REST CRUD API (`/tasks`) and an HTML/JS frontend | `./gradlew runLab2` |
| 2.2 | `com.vizja.lab3` | Same app plus HTTP cookie state management (server-set cookies, client-side JavaScript reads and writes them) | `./gradlew runLab3` |

Both labs listen on <http://localhost:8080>, so run one at a time. On Windows use `gradlew.bat`.

## Layout

```
.
|-- build.gradle, settings.gradle       one build for every lab (Lombok plugin, SLF4J, Jackson 3)
|-- gradlew, gradlew.bat, gradle/       Gradle 8.14 wrapper
|-- src/main/
|   |-- java/com/vizja/
|   |   |-- swp/lab2/                   Lab 2.1: Application, controller, model, repository, lib (framework)
|   |   `-- lab3/                       Lab 2.2: App, controller, model, repository, lib (framework)
|   `-- resources/simplelogger.properties
|-- http/                               ready-made requests for IntelliJ's HTTP client (lab2.http, lab3.http)
|-- docs/                               lab2-README.md, lab3-README.md (architecture, API, cookies, verification)
|-- submissions/                        the ZIPs that were handed in (application code only, no framework)
`-- Class resources/                    course preparation reading and course lists
```

Each lab has its own copy of the framework in its own `lib` package (the labs ship slightly different framework versions), so the labs never interfere with each other. Application code is everything outside `lib/`.

## Build and run

Requires JDK 17-21 (the bundled Gradle 8.14 supports up to Java 24).

```bash
./gradlew build        # compile everything
./gradlew runLab2      # or runLab3
```

You can also open this folder as a Gradle project in IntelliJ and run `com.vizja.swp.lab2.Application` (Lab 2.1) or `com.vizja.lab3.App` (Lab 2.2) directly.

## More detail

- [Lab 2.1 notes](docs/lab2-README.md): structure, REST API, frontend, known gaps
- [Lab 2.2 notes](docs/lab3-README.md): request flow, cookie lifecycle, client-side script, verification, security notes
