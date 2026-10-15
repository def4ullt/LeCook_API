package com.lecook.backend;

import com.lecook.backend.service.RecipeImageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in live checks. Database fixtures roll back; only this test's uploaded object is removed. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "LECOOK_NEON_SMOKE", matches = "true")
class NeonServicesTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RecipeImageService images;
    @Autowired Environment environment;
    @Value("${local.server.port}") int port;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    @Test @Transactional
    void postgresSupportsSharedIngredientsAcrossRecipes() {
        Integer category = jdbc.queryForObject("INSERT INTO categories(name) VALUES (?) RETURNING id", Integer.class, "Перевірка Neon " + UUID.randomUUID());
        Integer ingredient = jdbc.queryForObject("INSERT INTO ingredients(name) VALUES (?) RETURNING id", Integer.class, "Тестовий інгредієнт " + UUID.randomUUID());
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        for (UUID id : List.of(first, second)) {
            jdbc.update("INSERT INTO recipes(id,name,description,calories,category_id) VALUES (?,?,?,?,?)", id, "Тест зв’язку М–М", "Тимчасовий запис перевірки", 100, category);
            jdbc.update("INSERT INTO recipe_ingredients(recipe_id,ingredient_id,quantity) VALUES (?,?,?)", id, ingredient, 50);
        }
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM recipe_ingredients WHERE ingredient_id=?", Integer.class, ingredient));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM ingredients WHERE id=?", Integer.class, ingredient));
    }

    @Test void actualHttpServerReadsNeonAndRejectsUnauthenticatedWrites() throws Exception {
        var health = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/health")).timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, health.statusCode());
        assertTrue(health.body().contains("ok"));
        var rejected = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/recipes"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, rejected.statusCode());
        var synced = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/recipes/sync/page"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"knownIds\":[]}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, synced.statusCode());
    }

    @Test void imageUploadTravelsThroughHttpAndCanBeReadAnonymously() throws Exception {
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aWQAAAABJRU5ErkJggg==");
        String boundary = "lecook-" + UUID.randomUUID();
        var bytes = new java.io.ByteArrayOutputStream();
        bytes.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"verification.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        bytes.write(png);
        bytes.write(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var uploaded = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/admin/recipe-images"))
            .timeout(Duration.ofSeconds(45)).header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .header("Authorization", "Bearer " + environment.getRequiredProperty("LECOOK_ADMIN_TOKEN"))
            .POST(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, uploaded.statusCode());
        var result = new tools.jackson.databind.json.JsonMapper().readTree(uploaded.body());
        String key = result.get("key").asText();
        try {
            assertArrayEquals(png, images.download(key));
            var publicRead = http.send(HttpRequest.newBuilder(URI.create(result.get("url").asText())).timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, publicRead.statusCode());
            assertArrayEquals(png, publicRead.body());
        } finally { images.delete(key); }
    }
}
