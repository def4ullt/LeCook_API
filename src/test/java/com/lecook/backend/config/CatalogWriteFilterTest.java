package com.lecook.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;

class CatalogWriteFilterTest {
    private final String token = "test-token-012345678901234567890123456789";

    @Test void cloudSyncIsPublicButMutationsNeedAdministratorToken() throws Exception {
        var environment = new MockEnvironment().withProperty("lecook.cloud", "true").withProperty("LECOOK_ADMIN_TOKEN", token);
        var filter = new CatalogWriteFilter(environment);
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("POST", "/api/recipes/sync/page"), response, new MockFilterChain());
        assertEquals(200, response.getStatus());
        response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("POST", "/api/assistant"), response, new MockFilterChain());
        assertEquals(200, response.getStatus());
        response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("DELETE", "/api/recipes/id"), response, new MockFilterChain());
        assertEquals(401, response.getStatus());
        var request = new MockHttpServletRequest("POST", "/api/recipes");
        request.addHeader("Authorization", "Bearer " + token);
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(200, response.getStatus());
    }
    @Test void imageUploadsStayProtectedEvenForLocalBackend() throws Exception {
        var filter = new CatalogWriteFilter(new MockEnvironment());
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("POST", "/api/admin/recipe-images"), response, new MockFilterChain());
        assertEquals(503, response.getStatus());
        response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/admin/assistant/status"), response, new MockFilterChain());
        assertEquals(503, response.getStatus());
    }
}
