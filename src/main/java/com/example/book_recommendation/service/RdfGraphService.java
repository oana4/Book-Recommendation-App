package com.example.book_recommendation.service;

import com.example.book_recommendation.model.RdfEdge;
import com.example.book_recommendation.model.RdfGraph;
import com.example.book_recommendation.model.RdfNode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RdfGraphService {
    public RdfGraph parseRdfXml(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Node root = document.getDocumentElement();
            Set<String> nodes = new LinkedHashSet<>();
            List<RawEdge> rawEdges = new ArrayList<>();

            readRdfNodes(root, nodes, rawEdges);
            return buildGraph(nodes, rawEdges);
        } catch (Exception exception) {
            throw new IllegalArgumentException("The uploaded file could not be parsed as RDF/XML.");
        }
    }

    private void readRdfNodes(Node root, Set<String> nodes, List<RawEdge> rawEdges) {
        NodeList resources = root.getChildNodes();

        for (int index = 0; index < resources.getLength(); index++) {
            Node resource = resources.item(index);

            if (resource.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String subject = getResourceName((Element) resource);
            nodes.add(subject);
            NodeList properties = resource.getChildNodes();

            for (int propertyIndex = 0; propertyIndex < properties.getLength(); propertyIndex++) {
                Node property = properties.item(propertyIndex);

                if (property.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                String predicate = property.getLocalName() != null ? property.getLocalName() : property.getNodeName();
                String object = getObjectValue((Element) property);

                if (!object.isBlank()) {
                    nodes.add(object);
                    rawEdges.add(new RawEdge(subject, object, predicate));
                }
            }
        }
    }

    private RdfGraph buildGraph(Set<String> nodeLabels, List<RawEdge> rawEdges) {
        List<String> resources = nodeLabels.stream()
                .filter(label -> rawEdges.stream().anyMatch(edge -> edge.source().equals(label)))
                .toList();
        List<String> literals = nodeLabels.stream()
                .filter(label -> resources.stream().noneMatch(resource -> resource.equals(label)))
                .toList();
        Map<String, RdfNode> nodeMap = new LinkedHashMap<>();
        int rowCount = Math.max(resources.size(), literals.size());
        int width = 1180;
        int height = Math.max(420, 140 + rowCount * 96);
        int resourceSpacing = resources.size() <= 1 ? 0 : (height - 180) / (resources.size() - 1);
        int literalSpacing = literals.size() <= 1 ? 0 : (height - 180) / (literals.size() - 1);

        for (int index = 0; index < resources.size(); index++) {
            String label = resources.get(index);
            nodeMap.put(label, new RdfNode("node-" + index, label, "resource", 170, 90 + index * resourceSpacing));
        }

        for (int index = 0; index < literals.size(); index++) {
            String label = literals.get(index);
            nodeMap.put(label, new RdfNode("literal-" + index, label, "literal", 1010, 90 + index * literalSpacing));
        }

        List<RdfEdge> edges = rawEdges.stream()
                .map(edge -> {
                    RdfNode source = nodeMap.get(edge.source());
                    RdfNode target = nodeMap.get(edge.target());
                    return new RdfEdge(
                            edge.source(),
                            edge.target(),
                            edge.label(),
                            source.getX() + 75,
                            source.getY(),
                            target.getX() - 75,
                            target.getY()
                    );
                })
                .toList();

        List<RdfNode> graphNodes = new ArrayList<>(nodeMap.values());
        return new RdfGraph(graphNodes, edges, width, height, toNodesJson(graphNodes), toEdgesJson(edges));
    }

    private String toNodesJson(List<RdfNode> nodes) {
        StringBuilder json = new StringBuilder("[");

        for (int index = 0; index < nodes.size(); index++) {
            RdfNode node = nodes.get(index);
            json.append("{\"id\":\"")
                    .append(escapeJson(node.getLabel()))
                    .append("\",\"type\":\"")
                    .append(escapeJson(node.getType()))
                    .append("\"}");

            if (index < nodes.size() - 1) {
                json.append(",");
            }
        }

        return json.append("]").toString();
    }

    private String toEdgesJson(List<RdfEdge> edges) {
        StringBuilder json = new StringBuilder("[");

        for (int index = 0; index < edges.size(); index++) {
            RdfEdge edge = edges.get(index);
            json.append("{\"source\":\"")
                    .append(escapeJson(edge.getSource()))
                    .append("\",\"target\":\"")
                    .append(escapeJson(edge.getTarget()))
                    .append("\",\"label\":\"")
                    .append(escapeJson(edge.getLabel()))
                    .append("\"}");

            if (index < edges.size() - 1) {
                json.append(",");
            }
        }

        return json.append("]").toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String getResourceName(Element element) {
        String about = element.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");

        if (about == null || about.isBlank()) {
            NamedNodeMap attributes = element.getAttributes();

            for (int index = 0; index < attributes.getLength(); index++) {
                Node attribute = attributes.item(index);

                if (attribute.getNodeName().endsWith(":about")) {
                    about = attribute.getNodeValue();
                    break;
                }
            }
        }

        if (about != null && !about.isBlank()) {
            return shorten(about);
        }

        return element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
    }

    private String getObjectValue(Element element) {
        String resource = element.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");

        if (resource != null && !resource.isBlank()) {
            return shorten(resource);
        }

        return element.getTextContent().trim();
    }

    private String shorten(String value) {
        int hashIndex = value.lastIndexOf('#');
        int slashIndex = value.lastIndexOf('/');
        int separatorIndex = Math.max(hashIndex, slashIndex);

        if (separatorIndex >= 0 && separatorIndex < value.length() - 1) {
            return value.substring(separatorIndex + 1);
        }

        return value;
    }

    private record RawEdge(String source, String target, String label) {
    }
}
