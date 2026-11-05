package com.vizja.lab3;

import com.vizja.lab3.controller.HomeController;
import com.vizja.lab3.controller.TasksController;
import com.vizja.lab3.lib.FrontController;
import com.vizja.lab3.lib.Server;
import com.vizja.lab3.model.Task;
import com.vizja.lab3.repository.TaskRepository;

public class App {

    public static void main(String[] args) {
        TaskRepository repository = seedRepository();

        FrontController.addRoute("/", new HomeController());
        FrontController.addRoute("/tasks", new TasksController(repository));

        try (final var server = new Server()) {
            server.start(8080);
        }
    }

    /** Populates the repository with sample tasks so the UI is non-empty on first load. */
    private static TaskRepository seedRepository() {
        var repo = new TaskRepository();
        repo.save(new Task("Set up development environment", "Install JDK 21, configure Gradle, and verify the build passes.", Task.Status.DONE));
        repo.save(new Task("Implement REST API endpoints", "Create CRUD controllers for the task resource on top of the framework.", Task.Status.IN_PROGRESS));
        repo.save(new Task("Write integration tests", "Cover all four CRUD operations with real HTTP requests to localhost.", Task.Status.TODO));
        repo.save(new Task("Document the API", "Describe each endpoint, expected payloads, and status codes in the README.", Task.Status.TODO));
        return repo;
    }
}
