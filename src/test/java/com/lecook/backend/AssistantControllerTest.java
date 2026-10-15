package com.lecook.backend;

import com.lecook.backend.controller.AssistantController;
import com.lecook.backend.service.CookingAssistant;
import com.lecook.backend.exception.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AssistantControllerTest {
    @Test void blankAndOversizedQuestionsDoNotReachGemini() throws Exception {
        var assistant=mock(CookingAssistant.class);
        var mvc=MockMvcBuilders.standaloneSetup(new AssistantController(assistant)).setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(post("/api/assistant").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\" \"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/assistant").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\""+"x".repeat(1501)+"\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(assistant);
        when(assistant.answer("Смачна вечеря?", "")).thenReturn("Спробуйте овочеве рагу.");
        mvc.perform(post("/api/assistant").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Смачна вечеря?\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value("Спробуйте овочеве рагу."));
    }
}
