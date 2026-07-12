package com.lecook.backend.controller;

import com.lecook.backend.entity.Recipe;
import com.lecook.backend.entity.RecipeIngredient;
import com.lecook.backend.entity.RecipeIngredientId;
import com.lecook.backend.service.RecipeIngredientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recipe_ingredient")
public class RecipeIngredientController {

    private final RecipeIngredientService service;

    public RecipeIngredientController(RecipeIngredientService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecipeIngredient> getAll() {
        return service.getAll();
    }


    @PostMapping
    public RecipeIngredient create(@RequestBody RecipeIngredient recipeIngredient) {
        return service.create(recipeIngredient);
    }

    @GetMapping("/{id}")
    public RecipeIngredient get(@PathVariable RecipeIngredientId id) {
        return service.getById(id);
    }


    @PutMapping("/{id}")
    public RecipeIngredient update(@PathVariable RecipeIngredientId id, @RequestBody RecipeIngredient recipeIngredient) {
        return service.update(id, recipeIngredient);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable RecipeIngredientId id) {
        service.delete(id);
    }
}
