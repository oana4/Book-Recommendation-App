package com.example.book_recommendation.model;

public class RdfEdge {
    private final String source;
    private final String target;
    private final String label;
    private final int sourceX;
    private final int sourceY;
    private final int targetX;
    private final int targetY;
    private final int labelX;
    private final int labelY;

    public RdfEdge(String source, String target, String label, int sourceX, int sourceY, int targetX, int targetY) {
        this.source = source;
        this.target = target;
        this.label = label;
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.labelX = (sourceX + targetX) / 2;
        this.labelY = (sourceY + targetY) / 2;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getLabel() {
        return label;
    }

    public int getSourceX() {
        return sourceX;
    }

    public int getSourceY() {
        return sourceY;
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public int getLabelX() {
        return labelX;
    }

    public int getLabelY() {
        return labelY;
    }
}
