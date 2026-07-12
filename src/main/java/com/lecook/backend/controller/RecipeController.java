package com.lecook.backend.controller;

import com.lecook.backend.entity.Recipe;
import com.lecook.backend.service.RecipeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }


    @GetMapping
    public List<Recipe> getAll() {
        return service.getAll();
    }


    @PostMapping
    public Recipe create(@RequestBody Recipe recipe) {
        return service.create(recipe);
    }

    @GetMapping("/{id}")
    public Recipe get(@PathVariable UUID id) {
        return service.getById(id);
    }


    @PutMapping("/{id}")
    public Recipe update(@PathVariable UUID id, @RequestBody Recipe recipe) {
        return service.update(id, recipe);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
