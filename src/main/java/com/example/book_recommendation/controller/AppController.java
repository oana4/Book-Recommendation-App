package com.example.book_recommendation.controller;

import com.example.book_recommendation.model.User;
import com.example.book_recommendation.service.LibraryService;
import com.example.book_recommendation.service.RdfBookService;
import com.example.book_recommendation.service.RdfGraphService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AppController {
    private final LibraryService libraryService;
    private final RdfBookService rdfBookService;
    private final RdfGraphService rdfGraphService;

    public AppController(LibraryService libraryService, RdfBookService rdfBookService, RdfGraphService rdfGraphService) {
        this.libraryService = libraryService;
        this.rdfBookService = rdfBookService;
        this.rdfGraphService = rdfGraphService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("username") == null) {
            return "redirect:/login";
        }

        return "redirect:/recommendations";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        Optional<User> user = libraryService.authenticate(username, password);

        if (user.isEmpty()) {
            model.addAttribute("error", "Invalid username or password. Try Alice or Bob.");
            return "login";
        }

        session.setAttribute("username", user.get().getUsername());
        return "redirect:/recommendations";
    }

    @GetMapping("/recommendations")
    public String recommendations(HttpSession session, Model model) {
        Optional<User> user = getLoggedUser(session);

        if (user.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("user", user.get());
        model.addAttribute("recommendedBooks", libraryService.getRecommendedBooks(user.get()));
        model.addAttribute("themeMatches", libraryService.getThemeMatches(user.get()));
        return "recommendations";
    }

    @GetMapping("/books")
    public String books(HttpSession session, Model model) {
        Optional<User> user = getLoggedUser(session);

        if (user.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("user", user.get());
        model.addAttribute("books", rdfBookService.readBooks());
        return "books";
    }

    @GetMapping("/books/{title}")
    public String bookDetails(@PathVariable String title, HttpSession session, Model model) {
        Optional<User> user = getLoggedUser(session);

        if (user.isEmpty()) {
            return "redirect:/login";
        }

        Optional<com.example.book_recommendation.model.Book> book = rdfBookService.findBook(title);

        if (book.isEmpty()) {
            return "redirect:/books";
        }

        model.addAttribute("user", user.get());
        model.addAttribute("book", book.get());
        return "book-details";
    }

    @PostMapping("/books")
    public String addBook(
            @RequestParam String title,
            @RequestParam String themes,
            @RequestParam String readingLevel,
            @RequestParam String description,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Optional<User> user = getLoggedUser(session);

        if (user.isEmpty()) {
            return "redirect:/login";
        }

        try {
            rdfBookService.addBook(title, themes, readingLevel, description);
            redirectAttributes.addFlashAttribute("success", "Saved \"" + title.trim() + "\" to the RDF/XML library with Apache Jena.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }

        return "redirect:/books";
    }

    @PostMapping("/books/reading-level")
    public String updateReadingLevel(
            @RequestParam String title,
            @RequestParam String readingLevel,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Optional<User> user = getLoggedUser(session);

        if (user.isEmpty()) {
            return "redirect:/login";
        }

        try {
            rdfBookService.updateReadingLevel(title, readingLevel);
            redirectAttributes.addFlashAttribute("success", "Updated \"" + title.trim() + "\" to " + readingLevel.trim() + " in RDF/XML.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }

        return "redirect:/books";
    }

    @GetMapping("/rdf")
    public String rdfUploadPage(HttpSession session, Model model) {
        Optional<User> user = getLoggedUser(session);

        if (user.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("user", user.get());
        return "rdf";
    }

    @PostMapping("/rdf")
    public String visualizeRdf(@RequestParam MultipartFile file, HttpSession session, Model model) {
        Optional<User> user = getLoggedUser(session);

        if (user.isEmpty()) {
            return "redirect:/login";
        }

        model.addAttribute("user", user.get());

        if (file.isEmpty()) {
            model.addAttribute("error", "Please choose an RDF/XML file before visualizing the graph.");
            return "rdf";
        }

        try {
            model.addAttribute("graph", rdfGraphService.parseRdfXml(file));
            model.addAttribute("fileName", file.getOriginalFilename());
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
        }

        return "rdf";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private Optional<User> getLoggedUser(HttpSession session) {
        Object username = session.getAttribute("username");

        if (username == null) {
            return Optional.empty();
        }

        return libraryService.findUserByUsername(username.toString());
    }
}
