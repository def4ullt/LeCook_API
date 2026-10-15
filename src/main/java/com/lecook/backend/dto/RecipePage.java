package com.lecook.backend.dto;

import java.util.List;

public record RecipePage(List<RecipeResponse> recipes, int page, int size, long total) {}
