package com.lecook.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/** Public clients may read/sync; cloud catalog mutations require a server-side admin token. */
@Component
public class CatalogWriteFilter extends OncePerRequestFilter {
    private final Environment environment;
    public CatalogWriteFilter(Environment environment) { this.environment = environment; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean safe = Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod()) ||
            (request.getMethod().equals("POST") && Set.of("/api/recipes/sync", "/api/recipes/sync/page", "/api/assistant").contains(path));
        boolean protect = path.startsWith("/api/admin/") || (environment.getProperty("lecook.cloud", Boolean.class, false) && path.startsWith("/api/") && !safe);
        if (!protect) { chain.doFilter(request, response); return; }
        String token = environment.getProperty("LECOOK_ADMIN_TOKEN", "");
        if (token.length() < 32) { error(response, 503, "admin_not_configured"); return; }
        String header = request.getHeader("Authorization");
        String supplied = header != null && header.startsWith("Bearer ") && header.length() <= 1024 ? header.substring(7) : "";
        if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            error(response, 401, "unauthorized"); return;
        }
        chain.doFilter(request, response);
    }
    private void error(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status); response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"Catalog writes require administrator access\",\"fields\":{}}");
    }
}
