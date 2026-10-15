package com.lecook.backend.dto;

import jakarta.validation.constraints.*;

public record RecipeRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 20000) String description,
    @NotNull @Min(0) @Max(100000) Integer calories,
    @NotNull @Positive Integer categoryId,
    @Size(max = 255) @Pattern(regexp = "^(https?://[^\\s]+)?$", message = "must be an http(s) URL") String imageUrl
) {}
