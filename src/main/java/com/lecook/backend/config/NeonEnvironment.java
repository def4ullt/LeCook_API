package com.lecook.backend.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Loads CLI-managed dotenv files on the server only, without logging credentials. */
public class NeonEnvironment implements EnvironmentPostProcessor, Ordered {
    @Override public int getOrder() { return ConfigDataEnvironmentPostProcessor.ORDER + 1; }

    @Override public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        var values = new LinkedHashMap<String, Object>();
        var explicit = environment.getProperty("lecook.env-file");
        if (explicit != null) load(Path.of(explicit), values);
        else {
            var directory = Path.of(System.getProperty("user.dir"));
            if (directory.getFileName().toString().equals("backend")) directory = directory.getParent();
            load(directory.resolve(".env"), values);
            load(directory.resolve(".env.local"), values);
        }
        // Shell / hosting environment overrides dotenv. Generated Spring properties override local defaults.
        String database = value(environment, values, "DATABASE_URL");
        if (database != null && !database.isBlank()) {
            var jdbc = parseDatabaseUrl(database);
            values.put("spring.datasource.url", jdbc.url());
            values.put("spring.datasource.username", jdbc.username());
            values.put("spring.datasource.password", jdbc.password());
            String direct = value(environment, values, "DATABASE_URL_UNPOOLED");
            var migration = direct == null || direct.isBlank() ? jdbc : parseDatabaseUrl(direct);
            values.put("spring.flyway.url", migration.url());
            values.put("spring.flyway.user", migration.username());
            values.put("spring.flyway.password", migration.password());
            values.put("lecook.cloud", "true");
        }
        environment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new MapPropertySource("lecookNeon", values));
    }

    static void load(Path file, Map<String, Object> values) {
        if (!Files.isRegularFile(file)) return;
        try {
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("export ")) line = line.substring(7).strip();
                int separator = line.indexOf('=');
                if (separator < 1) continue;
                String key = line.substring(0, separator).strip();
                if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) continue;
                String value = line.substring(separator + 1).strip();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))))
                    value = value.substring(1, value.length() - 1);
                values.put(key, value);
            }
        } catch (java.io.IOException error) { throw new IllegalStateException("Could not read Neon environment file", error); }
    }

    private static String value(ConfigurableEnvironment environment, Map<String, Object> values, String key) {
        return environment.getProperty(key, (String) values.get(key));
    }

    public record JdbcConnection(String url, String username, String password) {
        @Override public String toString() { return "JdbcConnection[credentials redacted]"; }
    }

    static JdbcConnection parseDatabaseUrl(String value) {
        try {
            var uri = URI.create(value);
            if (!("postgresql".equals(uri.getScheme()) || "postgres".equals(uri.getScheme())) || uri.getHost() == null || uri.getRawUserInfo() == null || uri.getRawPath().length() <= 1 || uri.getFragment() != null)
                throw new IllegalArgumentException();
            var credentials = uri.getRawUserInfo().split(":", 2);
            if (credentials.length != 2) throw new IllegalArgumentException();
            String host = uri.getHost().contains(":") && !uri.getHost().startsWith("[") ? "[" + uri.getHost() + "]" : uri.getHost();
            String query = uri.getRawQuery() == null ? "" : uri.getRawQuery().replace("channel_binding=", "channelBinding=");
            if (!query.matches("(?s).*(^|&)sslmode=.*")) query += (query.isEmpty() ? "" : "&") + "sslmode=require";
            return new JdbcConnection("jdbc:postgresql://" + host + (uri.getPort() == -1 ? "" : ":" + uri.getPort()) + uri.getRawPath() + "?" + query,
                decode(credentials[0]), decode(credentials[1]));
        } catch (RuntimeException error) {
            // Never include the supplied URL or its parse exception: both can contain passwords.
            throw new IllegalArgumentException("Invalid DATABASE_URL; expected a PostgreSQL connection URL with username and password");
        }
    }
    private static String decode(String value) { return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8); }
}
