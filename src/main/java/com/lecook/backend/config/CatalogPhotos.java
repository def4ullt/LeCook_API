package com.lecook.backend.config;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.core.io.ClassPathResource;

/** Attribution travels with downloaded recipes, so it remains visible offline. */
public final class CatalogPhotos {
    private CatalogPhotos() {}
    public record Photo(String name, String filename, String credit, String source, String original) {}
    public static final List<Photo> ALL = load();
    private static List<Photo> load() {
        var resource = new ClassPathResource("catalog/photos.tsv");
        if (!resource.exists()) return List.of();
        try (var input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().filter(l -> !l.isBlank() && !l.startsWith("#"))
                .map(l -> l.split("\t", -1)).map(r -> new Photo(r[0],r[1],r[2],r[3],r[4])).toList();
        } catch (Exception error) { throw new IllegalStateException("Invalid photo manifest", error); }
    }
    public static String credit(String name) {
        return ALL.stream().filter(p -> p.name().equals(name)).findFirst().map(p -> p.credit() + "\n" + p.source()).orElse("");
    }
}
