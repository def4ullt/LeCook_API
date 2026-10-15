package com.lecook.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class HealthController {
    private final JdbcTemplate jdbc;
    public HealthController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    /** Process liveness only: health checks and uptime pings must not wake Neon. */
    @GetMapping("/api/ping") public Map<String, String> ping() { return Map.of("status", "ok"); }
    @GetMapping("/api/health") public ResponseEntity<Map<String, String>> health() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception error) {
            return ResponseEntity.status(503).body(Map.of("status", "unavailable"));
        }
    }
}
