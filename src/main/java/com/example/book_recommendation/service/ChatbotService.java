package com.example.book_recommendation.service;

import com.example.book_recommendation.model.Book;
import com.example.book_recommendation.model.User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatbotService {
    private final RdfBookService rdfBookService;

    public ChatbotService(RdfBookService rdfBookService) {
        this.rdfBookService = rdfBookService;
    }

    public String answer(String message, String pageContext, User user) {
        String safeMessage = message == null ? "" : message;
        List<Book> books = retrieveBooks(safeMessage + " " + pageContext, user);
        String lowerMessage = safeMessage.toLowerCase(Locale.ROOT);

        if (lowerMessage.contains("author") && lowerMessage.contains("theme")) {
            List<Book> matches = books.stream()
                    .filter(book -> containsIgnoreCase(safeMessage, book.getAuthor()))
                    .filter(book -> book.getThemes().stream().anyMatch(theme -> containsIgnoreCase(safeMessage, theme)))
                    .toList();

            if (!matches.isEmpty()) {
                return matches.stream().map(Book::getTitle).collect(Collectors.joining(", "));
            }
        }

        if (lowerMessage.contains("who wrote") || lowerMessage.contains("author")) {
            return books.stream()
                    .filter(book -> containsIgnoreCase(safeMessage, book.getTitle()))
                    .findFirst()
                    .map(book -> book.getTitle() + " was written by " + book.getAuthor() + ".")
                    .orElse("I found these relevant RDF books: " + titles(books) + ".");
        }

        if (lowerMessage.contains("enjoy") || lowerMessage.contains("recommend")) {
            return books.stream()
                    .filter(book -> book.getThemes().contains(user.getPreferredTheme()) || book.getReadingLevel().equalsIgnoreCase(user.getReadingLevel()))
                    .findFirst()
                    .map(book -> "You are most likely to enjoy " + book.getTitle() + " because it matches your profile: " + user.getPreferredTheme() + " / " + user.getReadingLevel() + ".")
                    .orElse("Based on the RDF vector database, the closest options are: " + titles(books) + ".");
        }

        if (books.isEmpty()) {
            return "I could not find a matching book in the RDF vector database.";
        }

        Book book = books.get(0);
        return "From the RDF vector database, the closest match is " + book.getTitle() + " by " + book.getAuthor()
                + ". Themes: " + String.join(", ", book.getThemes()) + ". Reading level: " + book.getReadingLevel() + ".";
    }

    private List<Book> retrieveBooks(String text, User user) {
        Set<String> queryTokens = tokenize(text + " " + user.getPreferredTheme() + " " + user.getReadingLevel());
        return rdfBookService.readBooks().stream()
                .sorted(Comparator.comparingInt((Book book) -> score(book, queryTokens, user)).reversed())
                .limit(4)
                .toList();
    }

    private int score(Book book, Set<String> queryTokens, User user) {
        Set<String> bookTokens = tokenize(book.getTitle() + " " + book.getAuthor() + " " + String.join(" ", book.getThemes()) + " " + book.getReadingLevel() + " " + book.getDescription());
        int score = (int) bookTokens.stream().filter(queryTokens::contains).count();

        if (book.getThemes().contains(user.getPreferredTheme())) {
            score += 2;
        }

        if (book.getReadingLevel().equalsIgnoreCase(user.getReadingLevel())) {
            score += 1;
        }

        return score;
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() > 2)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean containsIgnoreCase(String text, String value) {
        return text.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT));
    }

    private String titles(List<Book> books) {
        return books.stream().map(Book::getTitle).collect(Collectors.joining(", "));
    }
}
