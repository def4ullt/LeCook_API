package com.lecook.backend.service;

import com.lecook.backend.dto.*;
import com.lecook.backend.entity.*;
import com.lecook.backend.exception.*;
import com.lecook.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class RecipeIngredientService {
    private final RecipeIngredientRepository repository;
    private final RecipeRepository recipes;
    private final IngredientRepository ingredients;
    public RecipeIngredientService(RecipeIngredientRepository repository, RecipeRepository recipes, IngredientRepository ingredients) {
        this.repository = repository; this.recipes = recipes; this.ingredients = ingredients;
    }
    public List<RecipeIngredientResponse> list(UUID recipeId) {
        requiredRecipe(recipeId);
        return repository.forRecipe(recipeId).stream().map(RecipeIngredientResponse::from).toList();
    }
    @Transactional
    public RecipeIngredientResponse create(UUID recipeId, RecipeIngredientRequest request) {
        var recipe = requiredRecipe(recipeId);
        var ingredient = ingredients.findById(request.ingredientId()).orElseThrow(() -> new ResourceNotFoundException("Інгредієнт не знайдено"));
        var id = new RecipeIngredientId(recipeId, request.ingredientId());
        if (repository.existsById(id)) throw new ResourceConflictException("Цей інгредієнт уже додано до рецепта");
        var link = new RecipeIngredient(); link.setId(id); link.setRecipe(recipe); link.setIngredient(ingredient); link.setQuantity(request.quantity());
        link.setUnit(request.unit() == null ? "" : request.unit());
        return RecipeIngredientResponse.from(repository.saveAndFlush(link));
    }
    @Transactional
    public RecipeIngredientResponse update(UUID recipeId, Integer ingredientId, BigDecimal quantity) {
        var link = required(recipeId, ingredientId);
        link.setQuantity(quantity);
        return RecipeIngredientResponse.from(repository.saveAndFlush(link));
    }
    @Transactional
    public void delete(UUID recipeId, Integer ingredientId) {
        repository.delete(required(recipeId, ingredientId));
    }
    private Recipe requiredRecipe(UUID id) {
        return recipes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Рецепт не знайдено"));
    }
    private RecipeIngredient required(UUID recipeId, Integer ingredientId) {
        return repository.findById(new RecipeIngredientId(recipeId, ingredientId)).orElseThrow(() -> new ResourceNotFoundException("Інгредієнт рецепта не знайдено"));
    }
}
