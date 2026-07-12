package com.lecook.backend.service;


import com.lecook.backend.entity.Recipe;
import com.lecook.backend.entity.RecipeIngredient;
import com.lecook.backend.entity.RecipeIngredientId;
import com.lecook.backend.repository.RecipeIngredientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecipeIngredientService {

    private final RecipeIngredientRepository repository;

    public RecipeIngredientService(RecipeIngredientRepository repository) {
        this.repository = repository;
    }


    public List<RecipeIngredient> getAll() {
        return repository.findAll();
    }


    public RecipeIngredient create(RecipeIngredient recipeIngredient) {
        return repository.save(recipeIngredient);
    }

    public RecipeIngredient getById(RecipeIngredientId id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe's ingredient not found"));
    }

    public void delete(RecipeIngredientId id) {
        repository.deleteById(id);
    }

    public RecipeIngredient update(RecipeIngredientId id, RecipeIngredient recipeIngredient) {

        RecipeIngredient existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe's ingredient not found"));

        existing.setQuantity(recipeIngredient.getQuantity());

        return repository.save(existing);
    }
}
