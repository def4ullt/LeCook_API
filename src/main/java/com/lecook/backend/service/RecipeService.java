package com.lecook.backend.service;

import com.lecook.backend.dto.*;
import com.lecook.backend.entity.Recipe;
import com.lecook.backend.exception.ResourceNotFoundException;
import com.lecook.backend.repository.CategoryRepository;
import com.lecook.backend.repository.RecipeRepository;
import com.lecook.backend.repository.RecipeIngredientRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecipeService {
    private final RecipeRepository repository;
    private final CategoryRepository categories;
    private final RecipeIngredientRepository ingredients;
    private final RecipeDetailsService details;

    public RecipeService(RecipeRepository repository, CategoryRepository categories, RecipeIngredientRepository ingredients, RecipeDetailsService details) {
        this.repository = repository; this.categories = categories; this.ingredients = ingredients;
        this.details = details;
    }
    public RecipePage getAll(int page, int size) {
        var result = repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("ID"))));
        var compositions = details.load(result.stream().map(Recipe::getID).toList());
        return new RecipePage(result.map(recipe -> RecipeResponse.from(recipe, compositions.get(recipe.getID()))).getContent(), page, size, result.getTotalElements());
    }
    public RecipeResponse getById(UUID id) { return RecipeResponse.from(required(id), details.load(java.util.List.of(id)).get(id)); }

    @Transactional
    public RecipeResponse create(RecipeRequest request) {
        var recipe = new Recipe();
        apply(recipe, request);
        return RecipeResponse.from(repository.saveAndFlush(recipe));
    }
    @Transactional
    public RecipeResponse update(UUID id, RecipeRequest request) {
        var recipe = required(id);
        apply(recipe, request);
        return RecipeResponse.from(repository.saveAndFlush(recipe));
    }
    @Transactional
    public void delete(UUID id) {
        var recipe = required(id);
        ingredients.deleteForRecipe(id);
        repository.deleteById(id);
        repository.flush();
    }
    private Recipe required(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Рецепт не знайдено"));
    }
    private void apply(Recipe recipe, RecipeRequest request) {
        recipe.setName(request.name().trim()); recipe.setDescription(request.description().trim());
        recipe.setCalories(request.calories()); recipe.setImage_url(request.imageUrl());
        recipe.setCategory(categories.findById(request.categoryId()).orElseThrow(() -> new ResourceNotFoundException("Категорію не знайдено")));
    }
}
