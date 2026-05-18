package com.example.book_recommendation.model;

import java.util.List;

public class Book {
    private final String title;
    private final String author;
    private final List<String> themes;
    private final String readingLevel;
    private final String description;
    private final String accentColor;

    public Book(String title, String author, List<String> themes, String readingLevel, String description, String accentColor) {
        this.title = title;
        this.author = author;
        this.themes = themes;
        this.readingLevel = readingLevel;
        this.description = description;
        this.accentColor = accentColor;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public List<String> getThemes() {
        return themes;
    }

    public String getReadingLevel() {
        return readingLevel;
    }

    public String getDescription() {
        return description;
    }

    public String getAccentColor() {
        return accentColor;
    }
}
