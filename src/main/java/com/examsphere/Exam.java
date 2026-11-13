package com.examsphere;

/** Model class representing an examination. */
public class Exam {
    private final int id;
    private final String title;
    private final String description;
    private final int durationMinutes;

    public Exam(int id, String title, String description, int durationMinutes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getDurationMinutes() { return durationMinutes; }
}
