package com.lecook.backend.service;

import com.lecook.backend.dto.*;
import com.lecook.backend.repository.RecipeRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RecipeSyncService {
    private final RecipeRepository repository;
    private final RecipeDetailsService details;
    public RecipeSyncService(RecipeRepository repository, RecipeDetailsService details) { this.repository = repository; this.details = details; }

    public List<DownloadRecipe> download(SyncRequest request) {
        var recipes = request.knownIds().isEmpty() ? repository.findAll() : repository.findUnknown(request.knownIds());
        var compositions = details.load(recipes.stream().map(com.lecook.backend.entity.Recipe::getID).toList());
        return recipes.stream().map(recipe -> DownloadRecipe.from(recipe, compositions.get(recipe.getID()))).toList();
    }

    public SyncPage page(SyncRequest request) {
        var pageable = PageRequest.of(0, 201, Sort.by("ID"));
        var recipes = request.knownIds().isEmpty() ? repository.findAll(pageable).getContent() : repository.findUnknownPage(request.knownIds(), pageable);
        var compositions = details.load(recipes.stream().limit(200).map(com.lecook.backend.entity.Recipe::getID).toList());
        return new SyncPage(recipes.stream().limit(200).map(recipe -> DownloadRecipe.from(recipe, compositions.get(recipe.getID()))).toList(), recipes.size() > 200);
    }
}
