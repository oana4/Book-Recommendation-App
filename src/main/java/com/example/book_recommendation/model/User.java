package com.example.book_recommendation.model;

public class User {
    private final String username;
    private final String displayName;
    private final String password;
    private final String preferredTheme;
    private final String readingLevel;

    public User(String username, String displayName, String password, String preferredTheme, String readingLevel) {
        this.username = username;
        this.displayName = displayName;
        this.password = password;
        this.preferredTheme = preferredTheme;
        this.readingLevel = readingLevel;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPassword() {
        return password;
    }

    public String getPreferredTheme() {
        return preferredTheme;
    }

    public String getReadingLevel() {
        return readingLevel;
    }
}
