package com.example.book_recommendation.model;

import java.util.List;

public class RdfGraph {
    private final List<RdfNode> nodes;
    private final List<RdfEdge> edges;
    private final int width;
    private final int height;
    private final String nodesJson;
    private final String edgesJson;

    public RdfGraph(List<RdfNode> nodes, List<RdfEdge> edges, int width, int height, String nodesJson, String edgesJson) {
        this.nodes = nodes;
        this.edges = edges;
        this.width = width;
        this.height = height;
        this.nodesJson = nodesJson;
        this.edgesJson = edgesJson;
    }

    public List<RdfNode> getNodes() {
        return nodes;
    }

    public List<RdfEdge> getEdges() {
        return edges;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getNodesJson() {
        return nodesJson;
    }

    public String getEdgesJson() {
        return edgesJson;
    }
}
