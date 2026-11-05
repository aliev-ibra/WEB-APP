package com.vizja.lab3.repository;

import com.vizja.lab3.model.Task;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory repository for Task entities.
 * Uses a ConcurrentHashMap so concurrent requests are handled safely.
 */
public class TaskRepository {

    private final ConcurrentHashMap<Long, Task> store = new ConcurrentHashMap<>();

    /**
     * Returns all stored tasks. Order is not guaranteed.
     */
    public Collection<Task> findAll() {
        return store.values();
    }

    /**
     * Finds a task by its id.
     */
    public Optional<Task> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * Saves (inserts or replaces) a task by its id.
     */
    public Task save(Task task) {
        store.put(task.getId(), task);
        return task;
    }

    /**
     * Removes a task by id.
     *
     * @return true if the task existed and was removed, false if not found
     */
    public boolean delete(long id) {
        return store.remove(id) != null;
    }
}
