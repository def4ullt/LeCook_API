package com.lecook.backend.controller;


import com.lecook.backend.entity.Category;
import com.lecook.backend.entity.Ingredient;
import com.lecook.backend.entity.RecipeIngredient;
import com.lecook.backend.entity.RecipeIngredientId;
import com.lecook.backend.service.CategoryService;
import com.lecook.backend.service.IngredientService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredient")
public class IngredientController {

    private final IngredientService service;

    public IngredientController(IngredientService service) {
        this.service = service;
    }


    @GetMapping
    public List<Ingredient> getAll() {
        return service.getAll();
    }


    @PostMapping
    public Ingredient create(@RequestBody Ingredient ingredient) {
        return service.create(ingredient);
    }

    @GetMapping("/{id}")
    public Ingredient get(@PathVariable Integer id) {
        return service.getById(id);
    }


    @PutMapping("/{id}")
    public Ingredient update(@PathVariable Integer id, @RequestBody Ingredient ingredient) {
        return service.update(id, ingredient);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
