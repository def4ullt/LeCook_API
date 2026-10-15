package com.lecook.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NeonEnvironmentTest {
    @TempDir Path directory;

    @Test void convertsNeonUrlWithoutPuttingCredentialsInJdbcUrl() {
        var result = NeonEnvironment.parseDatabaseUrl("postgresql://user:p%40ss%2Bword@ep-example.neon.tech/neondb?sslmode=require&channel_binding=require");
        assertEquals("user", result.username());
        assertEquals("p@ss+word", result.password());
        assertEquals("jdbc:postgresql://ep-example.neon.tech/neondb?sslmode=require&channelBinding=require", result.url());
        assertFalse(result.toString().contains("p@ss"));
    }
    @Test void addsTlsAndPreservesLiteralPlusInPassword() {
        var result = NeonEnvironment.parseDatabaseUrl("postgres://user:a+b@example.com:5432/neondb");
        assertEquals("a+b", result.password());
        assertTrue(result.url().endsWith("?sslmode=require"));
    }
    @Test void invalidUrlDoesNotLeakCredentials() {
        var error = assertThrows(IllegalArgumentException.class, () -> NeonEnvironment.parseDatabaseUrl("postgres://user:super-secret@bad host/db"));
        assertFalse(error.getMessage().contains("super-secret"));
        assertNull(error.getCause());
    }
    @Test void readsQuotedDotenvAndIgnoresCommentsWithoutExecutingValues() throws Exception {
        var file = directory.resolve(".env.local");
        Files.writeString(file, "# comment\nexport DATABASE_URL=\"postgresql://user:pass@example.com/db\"\nAWS_SECRET_ACCESS_KEY='literal$(not-a-command)'\n");
        var values = new HashMap<String, Object>();
        NeonEnvironment.load(file, values);
        assertEquals("postgresql://user:pass@example.com/db", values.get("DATABASE_URL"));
        assertEquals("literal$(not-a-command)", values.get("AWS_SECRET_ACCESS_KEY"));
    }
}
