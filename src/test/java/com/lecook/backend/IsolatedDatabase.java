package com.lecook.backend;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.UUID;

/** Every test run uses a new schema. Existing application tables are never test fixtures. */
abstract class IsolatedDatabase {
    private static final String SCHEMA = "lecook_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final Properties CONFIG = config();
    private static final String URL = value("SPRING_DATASOURCE_URL", "spring.datasource.url");
    private static final String USER = value("SPRING_DATASOURCE_USERNAME", "spring.datasource.username");
    private static final String PASSWORD = value("SPRING_DATASOURCE_PASSWORD", "spring.datasource.password");
    private static boolean initialized;

    private static Properties config() {
        try (var input = IsolatedDatabase.class.getResourceAsStream("/application.properties")) {
            var result = new Properties(); result.load(input);
            // Developer credentials remain in an ignored file, never in repository resources.
            var local = java.nio.file.Path.of("local-db.properties");
            if (java.nio.file.Files.isRegularFile(local)) {
                try (var reader = java.nio.file.Files.newBufferedReader(local)) { result.load(reader); }
            }
            return result;
        } catch (Exception error) { throw new IllegalStateException("Cannot load test database configuration", error); }
    }
    private static String value(String environment, String key) {
        return System.getenv().getOrDefault(environment, CONFIG.getProperty(key));
    }
    @DynamicPropertySource
    static synchronized void database(DynamicPropertyRegistry properties) throws Exception {
        if (!initialized) {
            try (var connection = DriverManager.getConnection(URL, USER, PASSWORD); var sql = connection.createStatement()) {
                sql.execute("CREATE SCHEMA " + SCHEMA);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // The target is generated here, never supplied by application configuration.
                if (!SCHEMA.matches("lecook_test_[0-9a-f]{32}")) return;
                try (var connection = DriverManager.getConnection(URL, USER, PASSWORD); var sql = connection.createStatement()) {
                    sql.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
                } catch (Exception ignored) { }
            }));
            initialized = true;
        }
        properties.add("spring.datasource.url", () -> URL + (URL.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        properties.add("spring.datasource.username", () -> USER);
        properties.add("spring.datasource.password", () -> PASSWORD);
        properties.add("AWS_ENDPOINT_URL_S3", () -> "false");
        properties.add("spring.flyway.url", () -> URL + (URL.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        properties.add("spring.flyway.user", () -> USER);
        properties.add("spring.flyway.password", () -> PASSWORD);
        properties.add("lecook.cloud", () -> false);
        properties.add("lecook.env-file", () -> "disabled-for-isolated-tests.env");
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.baseline-on-migrate", () -> false);
        properties.add("spring.jpa.show-sql", () -> false);
    }
}
