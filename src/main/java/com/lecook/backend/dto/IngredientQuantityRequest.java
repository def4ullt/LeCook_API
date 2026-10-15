package com.lecook.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record IngredientQuantityRequest(@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal quantity) {}
