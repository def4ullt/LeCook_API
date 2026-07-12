package com.lecook.backend.service;

import com.lecook.backend.entity.Ingredient;
import com.lecook.backend.entity.Recipe;
import com.lecook.backend.entity.RecipeIngredient;
import com.lecook.backend.entity.RecipeIngredientId;
import com.lecook.backend.repository.IngredientRepository;
import com.lecook.backend.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngredientService {

    private final IngredientRepository repository;

    public IngredientService(IngredientRepository repository) {
        this.repository = repository;
    }


    public List<Ingredient> getAll() {
        return repository.findAll();
    }


    public Ingredient create(Ingredient ingredient) {
        return repository.save(ingredient);
    }

    public Ingredient getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }

    public Ingredient update(Integer id, Ingredient ingredient) {

        Ingredient existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));

        existing.setName(ingredient.getName());

        return repository.save(existing);
    }
}
