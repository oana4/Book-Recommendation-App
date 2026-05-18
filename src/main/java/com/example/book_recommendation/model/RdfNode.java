package com.example.book_recommendation.model;

public class RdfNode {
    private final String id;
    private final String label;
    private final String type;
    private final int x;
    private final int y;

    public RdfNode(String id, String label, String type, int x, int y) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
