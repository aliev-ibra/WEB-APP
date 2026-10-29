package com.vizja.swp.lab2.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single task entity in the task management system.
 * Instances are immutable except for mutable fields (title, description, status)
 * that can be updated via setters.
 */
public class Task {

    public enum Status {
        TODO, IN_PROGRESS, DONE
    }

    private static final AtomicLong ID_SEQUENCE = new AtomicLong(1);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final long id;
    private String title;
    private String description;
    private Status status;
    private final String createdAt;

    /**
     * Creates a new Task, automatically assigning an id and createdAt timestamp.
     */
    public Task(String title, String description, Status status) {
        this.id = ID_SEQUENCE.getAndIncrement();
        this.title = title;
        this.description = description != null ? description : "";
        this.status = status != null ? status : Status.TODO;
        this.createdAt = LocalDateTime.now().format(FORMATTER);
    }

    // --- Getters (used by Jackson for JSON serialization) ---

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // --- Setters for update operations ---

    public void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        this.title = title.trim();
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public void setStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        this.status = status;
    }
}
