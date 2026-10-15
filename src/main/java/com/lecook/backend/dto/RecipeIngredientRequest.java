package com.lecook.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RecipeIngredientRequest(@NotNull @Positive Integer ingredientId,
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal quantity,
    @Pattern(regexp = "^(г|мл|шт\\.|ч\\. л\\.|ст\\. л\\.|)$") String unit) {
    public RecipeIngredientRequest(Integer ingredientId, BigDecimal quantity) { this(ingredientId, quantity, ""); }
}
