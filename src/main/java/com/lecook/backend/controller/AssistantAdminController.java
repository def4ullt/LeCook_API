package com.lecook.backend.controller;

import com.lecook.backend.service.CookingAssistant;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** CatalogWriteFilter protects every /api/admin request, including GET. */
@RestController
public class AssistantAdminController {
    private final CookingAssistant assistant;
    public AssistantAdminController(CookingAssistant assistant) { this.assistant=assistant; }
    @GetMapping("/api/admin/assistant/status")
    public Map<String,Object> status() { return assistant.diagnostics(); }
}
