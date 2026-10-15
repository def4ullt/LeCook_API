package com.lecook.backend;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Read-only acceptance check against the selected Neon branch, through the actual HTTP server. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named="LECOOK_CATALOG_SMOKE", matches="true")
class CatalogCloudTest {
    @Value("${local.server.port}") int port;
    @Autowired ObjectMapper mapper;
    @Test void allSeededRecipesCanBeDownloadedWithIngredientsAndSteps() throws Exception {
        var client = HttpClient.newHttpClient();
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/recipes/sync/page"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"knownIds\":[]}")).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode());
        var body = mapper.readTree(response.body());
        assertFalse(body.get("hasMore").asBoolean());
        assertEquals(60, body.get("recipes").size());
        var ids = new ArrayList<String>();
        for (var recipe : body.get("recipes")) {
            ids.add(recipe.get("id").asText());
            assertTrue(recipe.get("calories").isNull());
            var details = recipe.get("details");
            assertTrue(details.get("servings").asInt()>0);
            assertEquals(3, details.get("steps").size());
            assertTrue(details.get("ingredients").size()>=4);
            for (var ingredient : details.get("ingredients")) {
                assertFalse(ingredient.get("unit").asText().isBlank());
                assertTrue(ingredient.get("quantity").asDouble()>0);
            }
        }
        var detail = client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/recipes/"+ids.get(0))).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, detail.statusCode());
        assertEquals(3, mapper.readTree(detail.body()).get("details").get("steps").size());
        var repeated = client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/recipes/sync/page"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("knownIds",ids)))).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, repeated.statusCode());
        assertEquals(0, mapper.readTree(repeated.body()).get("recipes").size());
        System.out.println("CATALOG HTTP VERIFIED: 60 recipes, structured ingredients, 180 steps, repeated sync=0");
    }
}
