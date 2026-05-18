package com.example.book_recommendation.service;

import com.example.book_recommendation.model.Book;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RdfBookService {
    private static final String REC = "http://example.com/book-recommendation#";
    private final Path rdfPath = Path.of("src", "main", "resources", "data", "books.rdf");

    public List<Book> readBooks() {
        Model model = readModel();
        String query = """
                PREFIX rec: <http://example.com/book-recommendation#>
                SELECT ?book ?title ?level
                WHERE {
                    ?book a rec:Book ;
                          rec:title ?title ;
                          rec:suitableReadingLevel ?level .
                }
                ORDER BY ?title
                """;
        Map<String, Book> books = new LinkedHashMap<>();

        try (QueryExecution queryExecution = QueryExecutionFactory.create(query, model)) {
            ResultSet results = queryExecution.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                Resource bookResource = solution.getResource("book");
                String title = solution.getLiteral("title").getString();
                books.put(title, toBook(model, bookResource));
            }
        }

        return new ArrayList<>(books.values());
    }

    public Optional<Book> findBook(String title) {
        Model model = readModel();
        return findBookByTitle(model, requireText(title, "Title"))
                .map(book -> toBook(model, book));
    }

    public void addBook(String title, String themesText, String readingLevel, String description) {
        String cleanTitle = requireText(title, "Title");
        List<String> themes = splitThemes(themesText);
        String cleanReadingLevel = requireText(readingLevel, "Reading level");
        String cleanDescription = requireText(description, "Description");

        if (themes.isEmpty()) {
            throw new IllegalArgumentException("Add at least one theme.");
        }

        Model model = readModel();
        Resource book = findBookByTitle(model, cleanTitle)
                .orElseGet(() -> model.createResource(REC + cleanTitle.replaceAll("[^A-Za-z0-9]", "")));

        replaceBookData(model, book, cleanTitle, themes, cleanReadingLevel, cleanDescription);
        saveModel(model);
    }

    public void updateReadingLevel(String title, String readingLevel) {
        String cleanTitle = requireText(title, "Title");
        String cleanReadingLevel = requireText(readingLevel, "Reading level");
        Model model = readModel();
        Resource book = findBookByTitle(model, cleanTitle)
                .orElseThrow(() -> new IllegalArgumentException("Book not found in RDF/XML."));

        Property levelProperty = model.createProperty(REC, "suitableReadingLevel");
        model.removeAll(book, levelProperty, null);
        model.add(book, levelProperty, cleanReadingLevel);
        saveModel(model);
    }

    private Model readModel() {
        try (InputStream inputStream = Files.newInputStream(rdfPath)) {
            Model model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, inputStream, Lang.RDFXML);
            return model;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read the RDF/XML file.");
        }
    }

    private void saveModel(Model model) {
        try (OutputStream outputStream = Files.newOutputStream(rdfPath)) {
            model.setNsPrefix("rec", REC);
            RDFDataMgr.write(outputStream, model, RDFFormat.RDFXML_PRETTY);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not write the RDF/XML file.");
        }
    }

    private Optional<Resource> findBookByTitle(Model model, String title) {
        Property titleProperty = model.createProperty(REC, "title");
        StmtIterator statements = model.listStatements(null, titleProperty, title);
        return statements.hasNext() ? Optional.of(statements.nextStatement().getSubject()) : Optional.empty();
    }

    private Book toBook(Model model, Resource book) {
        String title = readFirstValue(model, book, "title");
        String readingLevel = readFirstValue(model, book, "suitableReadingLevel");
        String author = readValueOrDefault(model, book, "author", "RDF Library");
        String description = readValueOrDefault(model, book, "description", "Stored in the RDF/XML file and managed with Apache Jena.");
        return new Book(
                title,
                author,
                readThemes(model, book),
                readingLevel,
                description,
                accentFor(readingLevel)
        );
    }

    private String readFirstValue(Model model, Resource book, String propertyName) {
        Property property = model.createProperty(REC, propertyName);
        return model.listObjectsOfProperty(book, property)
                .next()
                .asLiteral()
                .getString();
    }

    private String readValueOrDefault(Model model, Resource book, String propertyName, String defaultValue) {
        Property property = model.createProperty(REC, propertyName);
        if (!model.listObjectsOfProperty(book, property).hasNext()) {
            return defaultValue;
        }

        return model.listObjectsOfProperty(book, property)
                .next()
                .asLiteral()
                .getString();
    }

    private List<String> readThemes(Model model, Resource book) {
        Property themeProperty = model.createProperty(REC, "theme");
        return model.listObjectsOfProperty(book, themeProperty)
                .mapWith(node -> node.asLiteral().getString())
                .toList();
    }

    private void replaceBookData(Model model, Resource book, String title, List<String> themes, String readingLevel, String description) {
        Property titleProperty = model.createProperty(REC, "title");
        Property themeProperty = model.createProperty(REC, "theme");
        Property levelProperty = model.createProperty(REC, "suitableReadingLevel");
        Property descriptionProperty = model.createProperty(REC, "description");

        model.removeAll(book, null, null);
        model.add(book, RDF.type, model.createResource(REC + "Book"));
        model.add(book, titleProperty, title);
        themes.forEach(theme -> model.add(book, themeProperty, theme));
        model.add(book, levelProperty, readingLevel);
        model.add(book, descriptionProperty, description);
    }

    private List<String> splitThemes(String themesText) {
        return Arrays.stream(requireText(themesText, "Themes").split(","))
                .map(String::trim)
                .filter(theme -> !theme.isBlank())
                .toList();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        return value.trim();
    }

    private String accentFor(String readingLevel) {
        return switch (readingLevel.toLowerCase()) {
            case "beginner" -> "#5eead4";
            case "intermediate" -> "#9b8cff";
            case "advanced" -> "#ff8fb3";
            default -> "#ffd166";
        };
    }
}
