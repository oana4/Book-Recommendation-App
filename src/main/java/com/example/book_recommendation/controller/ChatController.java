package com.example.book_recommendation.controller;

import com.example.book_recommendation.model.User;
import com.example.book_recommendation.service.ChatbotService;
import com.example.book_recommendation.service.LibraryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
public class ChatController {
    private final ChatbotService chatbotService;
    private final LibraryService libraryService;

    public ChatController(ChatbotService chatbotService, LibraryService libraryService) {
        this.chatbotService = chatbotService;
        this.libraryService = libraryService;
    }

    @PostMapping("/api/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest request, HttpSession session) {
        Object username = session.getAttribute("username");

        if (username == null) {
            return ResponseEntity.ok(Map.of("answer", "Please log in before using the chatbot."));
        }

        Optional<User> user = libraryService.findUserByUsername(username.toString());

        if (user.isEmpty()) {
            return ResponseEntity.ok(Map.of("answer", "Please log in before using the chatbot."));
        }

        try {
            String answer = chatbotService.answer(request.message(), request.pageContext(), user.get());
            return ResponseEntity.ok(Map.of("answer", answer));
        } catch (RuntimeException exception) {
            return ResponseEntity.ok(Map.of("answer", "I could not read the RDF vector database right now."));
        }
    }

    public record ChatRequest(String message, String pageContext) {
    }
}
