package com.example.reminder;

public class ReminderModel {
    private final String title;
    private final String items;

    public ReminderModel(String title, String items) {
        this.title = title;
        this.items = items;
    }

    public String getTitle() { return title; }
    public String getItems() { return items; }
}