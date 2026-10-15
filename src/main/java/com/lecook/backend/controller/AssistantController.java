package com.lecook.backend.controller;

import com.lecook.backend.service.CookingAssistant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final CookingAssistant assistant;
    public AssistantController(CookingAssistant assistant) { this.assistant = assistant; }
    public record Question(@NotBlank @Size(max=1500) String message, @Size(max=8000) String context) {}
    @GetMapping("/status") public Map<String, Boolean> status() { return Map.of("available", assistant.available()); }
    @PostMapping public Map<String, String> ask(@Valid @RequestBody Question question) {
        return Map.of("answer", assistant.answer(question.message(), question.context() == null ? "" : question.context()));
    }
}
