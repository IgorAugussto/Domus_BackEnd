package com.igorAugusto.domus.domus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igorAugusto.domus.domus.dto.OutgoingRequest;
import com.igorAugusto.domus.domus.dto.OutgoingResponse;
import com.igorAugusto.domus.domus.enums.Frequency;
import com.igorAugusto.domus.domus.exception.BusinessException;
import com.igorAugusto.domus.domus.exception.ForbiddenException;
import com.igorAugusto.domus.domus.exception.ResourceNotFoundException;
import com.igorAugusto.domus.domus.service.JwtService;
import com.igorAugusto.domus.domus.service.OutgoingService;
import com.igorAugusto.domus.domus.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.igorAugusto.domus.domus.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = OutgoingController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class OutgoingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OutgoingService outgoingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    private OutgoingRequest validRequest;
    private OutgoingResponse outgoingResponse;

    private static final String USER_EMAIL = "test@domus.app";

    @BeforeEach
    void setUp() {
        validRequest = new OutgoingRequest(
                BigDecimal.valueOf(1500.00),
                "Aluguel",
                LocalDate.of(2026, 1, 1),
                12,
                Frequency.MONTHLY,
                "Moradia",
                "Boleto",
                false,
                null
        );

        outgoingResponse = new OutgoingResponse(
                1L,
                BigDecimal.valueOf(1500.00),
                "Aluguel",
                LocalDate.of(2026, 1, 1),
                12,
                "Moradia",
                LocalDateTime.now(),
                Frequency.MONTHLY,
                "Boleto",
                false,
                null
        );
    }

    // ─── POST /api/costs ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void createOutgoing_authenticatedWithValidBody_returns200WithResponse() throws Exception {
        when(outgoingService.createOutgoing(any(OutgoingRequest.class), eq(USER_EMAIL)))
                .thenReturn(outgoingResponse);

        mockMvc.perform(post("/api/costs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Aluguel"))
                .andExpect(jsonPath("$.value").value(1500.00))
                .andExpect(jsonPath("$.durationInMonths").value(12));

        verify(outgoingService, times(1)).createOutgoing(any(OutgoingRequest.class), eq(USER_EMAIL));
    }

    @Test
    void createOutgoing_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(post("/api/costs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().is4xxClientError());

        verify(outgoingService, never()).createOutgoing(any(), any());
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void createOutgoing_missingRequiredFields_returns400() throws Exception {
        // value e description são @NotNull/@NotBlank — corpo sem eles dispara validação
        String bodyWithoutRequiredFields = """
                {
                    "startDate": "2026-01-01",
                    "frequency": "Monthly"
                }
                """;

        mockMvc.perform(post("/api/costs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutRequiredFields))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void createOutgoing_monthlyWithoutDuration_returns400() throws Exception {
        when(outgoingService.createOutgoing(any(OutgoingRequest.class), eq(USER_EMAIL)))
                .thenThrow(new BusinessException("Duração é obrigatória para despesas recorrentes"));

        mockMvc.perform(post("/api/costs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Duração é obrigatória para despesas recorrentes"));
    }

    // ─── GET /api/costs ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void getAllOutgoings_authenticated_returns200WithPage() throws Exception {
        Page<OutgoingResponse> page = new PageImpl<>(List.of(outgoingResponse));

        when(outgoingService.getAllOutgoings(eq(USER_EMAIL), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/costs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].description").value("Aluguel"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllOutgoings_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/costs"))
                .andExpect(status().is4xxClientError());

        verify(outgoingService, never()).getAllOutgoings(any(), any());
    }

    // ─── GET /api/costs/total ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void getTotalOutgoing_authenticated_returns200WithTotal() throws Exception {
        when(outgoingService.getTotalOutgoing(USER_EMAIL)).thenReturn(BigDecimal.valueOf(3000.00));

        mockMvc.perform(get("/api/costs/total"))
                .andExpect(status().isOk())
                .andExpect(content().string("3000.0"));
    }

    // ─── PUT /api/costs/{id} ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void updateOutgoing_authenticated_returns200WithUpdatedResponse() throws Exception {
        when(outgoingService.updateOutgoing(eq(1L), any(OutgoingRequest.class), eq(USER_EMAIL)))
                .thenReturn(outgoingResponse);

        mockMvc.perform(put("/api/costs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void updateOutgoing_belongsToOtherUser_returns403() throws Exception {
        when(outgoingService.updateOutgoing(eq(1L), any(OutgoingRequest.class), eq(USER_EMAIL)))
                .thenThrow(new ForbiddenException("Acesso negado"));

        mockMvc.perform(put("/api/costs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado"));
    }

    // ─── DELETE /api/costs/{id} ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void deleteOutgoing_authenticated_returns204() throws Exception {
        doNothing().when(outgoingService).deleteOutgoing(1L, USER_EMAIL);

        mockMvc.perform(delete("/api/costs/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(outgoingService, times(1)).deleteOutgoing(1L, USER_EMAIL);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void deleteOutgoing_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Despesa não encontrada"))
                .when(outgoingService).deleteOutgoing(99L, USER_EMAIL);

        mockMvc.perform(delete("/api/costs/99").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Despesa não encontrada"));
    }
}
