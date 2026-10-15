package com.lecook.backend.dto;

import com.lecook.backend.entity.Recipe;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecipeResponse(UUID id, String name, String description, Integer calories,
    Integer categoryId, String category, String imageUrl, LocalDateTime createdAt, LocalDateTime updatedAt,
    com.lecook.backend.service.RecipeDetailsService.Details details) {
    public static RecipeResponse from(Recipe recipe) {
        return from(recipe, null);
    }
    public static RecipeResponse from(Recipe recipe, com.lecook.backend.service.RecipeDetailsService.Details details) {
        return new RecipeResponse(recipe.getID(), recipe.getName(), recipe.getDescription(), recipe.getCalories(),
            recipe.getCategory() == null ? null : recipe.getCategory().getId(),
            recipe.getCategory() == null ? "Інше" : recipe.getCategory().getName(),
            recipe.getImage_url(), recipe.getCreatedAt(), recipe.getUpdatedAt(), details);
    }
}
