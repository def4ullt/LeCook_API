package com.lecook.backend;

import com.lecook.backend.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthControllerTest {
    @Test void uptimePingDoesNotTouchDatabase() {
        var jdbc = mock(JdbcTemplate.class);
        assertEquals("ok", new HealthController(jdbc).ping().get("status"));
        verifyNoInteractions(jdbc);
    }
}
