package com.lecook.backend.controller;

import com.lecook.backend.dto.*;
import com.lecook.backend.service.RecipeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    private final RecipeService service;
    public RecipeController(RecipeService service) { this.service = service; }

    @GetMapping
    public RecipePage getAll(@RequestParam(defaultValue = "0") @Min(0) int page,
                             @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return service.getAll(page, size);
    }
    @PostMapping
    public ResponseEntity<RecipeResponse> create(@Valid @RequestBody RecipeRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/recipes/" + result.id())).body(result);
    }
    @GetMapping("/{id}") public RecipeResponse get(@PathVariable UUID id) { return service.getById(id); }
    @PutMapping("/{id}") public RecipeResponse update(@PathVariable UUID id, @Valid @RequestBody RecipeRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable UUID id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
