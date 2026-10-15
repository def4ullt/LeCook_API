package com.lecook.backend.dto;

import com.lecook.backend.entity.Recipe;
import java.util.UUID;

public record DownloadRecipe(UUID id, String name, String description, String category, Integer calories,
        com.lecook.backend.service.RecipeDetailsService.Details details, String imageUrl, String imageCredit, long publishedAt,Double protein,Double fat,Double carbs) {
    public static DownloadRecipe from(Recipe recipe) {
        return from(recipe, null);
    }
    public static DownloadRecipe from(Recipe recipe, com.lecook.backend.service.RecipeDetailsService.Details details) {
        return new DownloadRecipe(recipe.getID(), recipe.getName(),
            recipe.getDescription() == null || recipe.getDescription().isBlank() ? "Опис ще не додано" : recipe.getDescription(),
            recipe.getCategory() == null ? "Інше" : recipe.getCategory().getName(),
            recipe.getCalories(), details, recipe.getImage_url(), com.lecook.backend.config.CatalogPhotos.credit(recipe.getName()),
            recipe.getCreatedAt() == null ? 0 : recipe.getCreatedAt().toInstant(java.time.ZoneOffset.UTC).toEpochMilli(),recipe.getProtein(),recipe.getFat(),recipe.getCarbs());
    }
}
