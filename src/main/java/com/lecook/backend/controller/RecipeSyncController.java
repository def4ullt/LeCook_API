package com.lecook.backend.controller;

import com.lecook.backend.dto.*;
import com.lecook.backend.service.RecipeSyncService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recipes/sync")
public class RecipeSyncController {
    private final RecipeSyncService service;
    public RecipeSyncController(RecipeSyncService service) { this.service = service; }
    @PostMapping public List<DownloadRecipe> download(@Valid @RequestBody SyncRequest request) { return service.download(request); }
    @PostMapping("/page") public SyncPage page(@Valid @RequestBody SyncRequest request) { return service.page(request); }
}
