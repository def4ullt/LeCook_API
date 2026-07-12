package com.lecook.backend.service;

import com.lecook.backend.entity.Recipe;
import com.lecook.backend.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RecipeService {

    private final RecipeRepository repository;

    public RecipeService(RecipeRepository repository) {
        this.repository = repository;
    }


    public List<Recipe> getAll() {
        return repository.findAll();
    }


    public Recipe create(Recipe recipe) {
        return repository.save(recipe);
    }

    public Recipe getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public Recipe update(UUID id, Recipe recipe) {

        Recipe existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        existing.setName(recipe.getName());
        existing.setDescription(recipe.getDescription());
        existing.setCalories(recipe.getCalories());
        existing.setCategory(recipe.getCategory());
        existing.setUpdatedAt(LocalDateTime.now());

        return repository.save(existing);
    }
}