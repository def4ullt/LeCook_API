package com.lecook.backend.controller;

import com.lecook.backend.dto.*;
import com.lecook.backend.service.RecipeIngredientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/recipes/{recipeId}/ingredients")
public class RecipeIngredientController {
    private final RecipeIngredientService service;
    public RecipeIngredientController(RecipeIngredientService service) { this.service = service; }
    @GetMapping public List<RecipeIngredientResponse> list(@PathVariable UUID recipeId) { return service.list(recipeId); }
    @PostMapping public ResponseEntity<RecipeIngredientResponse> create(@PathVariable UUID recipeId, @Valid @RequestBody RecipeIngredientRequest request) {
        var result = service.create(recipeId, request);
        return ResponseEntity.created(URI.create("/api/recipes/" + recipeId + "/ingredients/" + result.ingredientId())).body(result);
    }
    @PutMapping("/{ingredientId}") public RecipeIngredientResponse update(@PathVariable UUID recipeId, @PathVariable Integer ingredientId, @Valid @RequestBody IngredientQuantityRequest request) {
        return service.update(recipeId, ingredientId, request.quantity());
    }
    @DeleteMapping("/{ingredientId}") public ResponseEntity<Void> delete(@PathVariable UUID recipeId, @PathVariable Integer ingredientId) {
        service.delete(recipeId, ingredientId); return ResponseEntity.noContent().build();
    }
}
