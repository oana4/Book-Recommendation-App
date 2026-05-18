package com.example.book_recommendation.service;

import com.example.book_recommendation.model.Book;
import com.example.book_recommendation.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LibraryService {
    private final List<User> users = List.of(
            new User("alice", "Alice", "alice123", "Science Fiction", "Intermediate"),
            new User("bob", "Bob", "bob123", "Mystery", "Beginner")
    );

    private final List<Book> books = List.of(
            new Book(
                    "Dune",
                    "Frank Herbert",
                    List.of("Science Fiction", "Fantasy"),
                    "Advanced",
                    "A legendary desert-world epic about politics, destiny, ecology, and power.",
                    "#ff8fb3"
            ),
            new Book(
                    "The Silent Patient",
                    "Alex Michaelides",
                    List.of("Mystery", "Murder"),
                    "Intermediate",
                    "A psychological mystery about silence, obsession, and a shocking truth.",
                    "#9b8cff"
            ),
            new Book(
                    "Hunger Games",
                    "Suzanne Collins",
                    List.of("Science Fiction", "Fantasy"),
                    "Beginner",
                    "A fast-paced dystopian story about survival, courage, and rebellion.",
                    "#5eead4"
            )
    );

    public Optional<User> authenticate(String username, String password) {
        return users.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .filter(user -> user.getPassword().equals(password))
                .findFirst();
    }

    public Optional<User> findUserByUsername(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public List<Book> getAllBooks() {
        return books;
    }

    public List<Book> getRecommendedBooks(User user) {
        return books.stream()
                .filter(book -> book.getThemes().contains(user.getPreferredTheme()))
                .filter(book -> book.getReadingLevel().equalsIgnoreCase(user.getReadingLevel()))
                .toList();
    }

    public List<Book> getThemeMatches(User user) {
        return books.stream()
                .filter(book -> book.getThemes().contains(user.getPreferredTheme()))
                .toList();
    }
}
