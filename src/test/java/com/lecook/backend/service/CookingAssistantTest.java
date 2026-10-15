package com.lecook.backend.service;

import java.io.*;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CookingAssistantTest {
    @Test void discoversAnAvailableModelInsteadOfDependingOnAnAlias() throws Exception {
        var http=mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<InputStream> listing=mock(HttpResponse.class);
        @SuppressWarnings("unchecked") HttpResponse<InputStream> answer=mock(HttpResponse.class);
        when(listing.statusCode()).thenReturn(200);
        when(listing.body()).thenReturn(new ByteArrayInputStream("{\"models\":[{\"name\":\"models/gemini-2.5-flash\",\"supportedGenerationMethods\":[\"generateContent\"]}]}".getBytes()));
        when(answer.statusCode()).thenReturn(200);
        when(answer.body()).thenReturn(new ByteArrayInputStream("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Soup\"}]}}]}".getBytes()));
        when(http.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any())).thenAnswer(invocation->{
            HttpRequest request=invocation.getArgument(0);
            if(request.method().equals("GET")) return listing;
            assertTrue(request.uri().getPath().endsWith("gemini-2.5-flash:generateContent"));
            return answer;
        });
        var assistant=new CookingAssistant(new MockEnvironment().withProperty("GEMINI_API_KEY","unit-test-secret"),new ObjectMapper(),http);
        assertEquals("Soup",assistant.answer("Dinner?", ""));
        assertEquals("gemini-2.5-flash",assistant.diagnostics().get("model"));
    }
    @Test void missingKeyMakesNoProviderRequest() {
        var http=mock(HttpClient.class);
        var assistant=new CookingAssistant(new MockEnvironment(),new ObjectMapper(),http);
        assertFalse(assistant.available());
        assertEquals(503,assertThrows(ResponseStatusException.class,()->assistant.answer("Вечеря?", "")).getStatusCode().value());
        verifyNoInteractions(http);
    }
    @Test void requestUsesSecretHeaderAndReturnsOnlyPublicAnswer() throws Exception {
        var http=mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<InputStream> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(new ByteArrayInputStream("{\"candidates\":[{\"content\":{\"parts\":[{\"thought\":true,\"text\":\"private\"},{\"text\":\"Додайте йогурт.\"}]}}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        when(http.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any())).thenAnswer(invocation->{
            HttpRequest request=invocation.getArgument(0);
            assertFalse(request.uri().toString().contains("unit-test-secret"));
            assertEquals("unit-test-secret",request.headers().firstValue("x-goog-api-key").orElseThrow());
            return response;
        });
        var assistant=new CookingAssistant(new MockEnvironment().withProperty("GEMINI_API_KEY","unit-test-secret").withProperty("GEMINI_MODEL","gemini-unit-test"),new ObjectMapper(),http);
        assertEquals("Додайте йогурт.",assistant.answer("Чим замінити сметану?", "Сирники"));
    }
    @Test void globalDailyLimitRejectsBeforeProviderIsCalled() {
        var assistant=new CookingAssistant(new MockEnvironment().withProperty("LECOOK_AI_DAILY_LIMIT","2"),new ObjectMapper(),mock(HttpClient.class));
        assistant.reserve();assistant.reserve();
        assertEquals(429,assertThrows(ResponseStatusException.class,assistant::reserve).getStatusCode().value());
    }
    @Test void providerErrorNeverLeaksItsBodyOrKey() throws Exception {
        var http=mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<InputStream> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(403);
        when(response.body()).thenReturn(new ByteArrayInputStream("secret-provider-diagnostic".getBytes()));
        when(http.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any())).thenReturn(response);
        var assistant=new CookingAssistant(new MockEnvironment().withProperty("GEMINI_API_KEY","unit-test-secret").withProperty("GEMINI_MODEL","gemini-unit-test"),new ObjectMapper(),http);
        var error=assertThrows(ResponseStatusException.class,()->assistant.answer("Вечеря?", ""));
        assertEquals(502,error.getStatusCode().value());
        assertFalse(error.getMessage().contains("secret"));
    }
}
