package com.backend.clinic.Controller;

import com.backend.clinic.DTO.ChatDTOs.ChatRequest;
import com.backend.clinic.DTO.ChatDTOs.ChatResponse;
import com.backend.clinic.Service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        // Get logged in username from JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        ChatResponse response = geminiService.processChat(request, username);
        return ResponseEntity.ok(response);
    }
}
