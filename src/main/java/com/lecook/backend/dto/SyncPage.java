package com.lecook.backend.dto;

import java.util.List;

public record SyncPage(List<DownloadRecipe> recipes, boolean hasMore) {}
