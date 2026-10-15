package com.lecook.backend.dto;

import com.lecook.backend.entity.RecipeIngredient;
import java.math.BigDecimal;
import java.util.UUID;

public record RecipeIngredientResponse(UUID recipeId, Integer ingredientId, String name, BigDecimal quantity, String unit) {
    public static RecipeIngredientResponse from(RecipeIngredient value) {
        return new RecipeIngredientResponse(value.getRecipe().getID(), value.getIngredient().getId(), value.getIngredient().getName(), value.getQuantity(), value.getUnit());
    }
}
