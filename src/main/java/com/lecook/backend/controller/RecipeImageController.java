package com.lecook.backend.controller;

import com.lecook.backend.service.RecipeImageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/admin/recipe-images")
@ConditionalOnProperty(name = "AWS_ENDPOINT_URL_S3")
public class RecipeImageController {
    private final RecipeImageService images;
    public RecipeImageController(RecipeImageService images) { this.images = images; }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecipeImageService.UploadedImage> upload(@RequestPart("file") MultipartFile file) throws IOException {
        var result = images.upload(file.getBytes());
        return ResponseEntity.created(URI.create(result.url())).body(result);
    }
}
