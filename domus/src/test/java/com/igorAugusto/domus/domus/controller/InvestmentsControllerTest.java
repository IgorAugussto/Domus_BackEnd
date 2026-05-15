package com.igorAugusto.domus.domus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igorAugusto.domus.domus.dto.InvestmentsRequest;
import com.igorAugusto.domus.domus.dto.InvestmentsResponse;
import com.igorAugusto.domus.domus.exception.ForbiddenException;
import com.igorAugusto.domus.domus.exception.ResourceNotFoundException;
import com.igorAugusto.domus.domus.service.InvestmentsService;
import com.igorAugusto.domus.domus.service.JwtService;
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

@WebMvcTest(value = InvestmentsController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class InvestmentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InvestmentsService investmentsService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    private InvestmentsRequest validRequest;
    private InvestmentsResponse investmentsResponse;

    private static final String USER_EMAIL = "test@domus.app";

    @BeforeEach
    void setUp() {
        validRequest = new InvestmentsRequest(
                BigDecimal.valueOf(1000.00),
                "ITAUSA4",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                "Ação Itaúsa",
                12.5
        );

        investmentsResponse = new InvestmentsResponse(
                1L,
                BigDecimal.valueOf(1000.00),
                "ITAUSA4",
                LocalDateTime.now(),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                12.5,
                "Ação Itaúsa"
        );
    }

    // ─── POST /api/investments ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void createInvestment_authenticatedWithValidBody_returns200WithResponse() throws Exception {
        when(investmentsService.createInvestment(any(InvestmentsRequest.class), eq(USER_EMAIL)))
                .thenReturn(investmentsResponse);

        mockMvc.perform(post("/api/investments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.typeInvestments").value("ITAUSA4"))
                .andExpect(jsonPath("$.value").value(1000.00))
                .andExpect(jsonPath("$.expectedReturn").value(12.5));

        verify(investmentsService, times(1)).createInvestment(any(InvestmentsRequest.class), eq(USER_EMAIL));
    }

    @Test
    void createInvestment_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(post("/api/investments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().is4xxClientError());

        verify(investmentsService, never()).createInvestment(any(), any());
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void createInvestment_missingRequiredFields_returns400() throws Exception {
        // value, typeInvestments, startDate e endDate são @NotNull
        String bodyWithoutValue = """
                {
                    "typeInvestments": "ITAUSA4",
                    "startDate": "2026-01-01",
                    "endDate": "2027-01-01"
                }
                """;

        mockMvc.perform(post("/api/investments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutValue))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /api/investments ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void getAllInvestments_authenticated_returns200WithPage() throws Exception {
        Page<InvestmentsResponse> page = new PageImpl<>(List.of(investmentsResponse));

        when(investmentsService.getAllInvestments(eq(USER_EMAIL), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/investments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].typeInvestments").value("ITAUSA4"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllInvestments_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/investments"))
                .andExpect(status().is4xxClientError());

        verify(investmentsService, never()).getAllInvestments(any(), any());
    }

    // ─── GET /api/investments/total ───────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void getTotalInvestments_authenticated_returns200WithTotal() throws Exception {
        when(investmentsService.getTotalInvestments(USER_EMAIL)).thenReturn(BigDecimal.valueOf(10000.00));

        mockMvc.perform(get("/api/investments/total"))
                .andExpect(status().isOk())
                .andExpect(content().string("10000.0"));
    }

    // ─── PUT /api/investments/{id} ────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void updateInvestment_authenticated_returns200WithUpdatedResponse() throws Exception {
        when(investmentsService.updateInvestment(eq(1L), any(InvestmentsRequest.class), eq(USER_EMAIL)))
                .thenReturn(investmentsResponse);

        mockMvc.perform(put("/api/investments/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void updateInvestment_belongsToOtherUser_returns403() throws Exception {
        when(investmentsService.updateInvestment(eq(1L), any(InvestmentsRequest.class), eq(USER_EMAIL)))
                .thenThrow(new ForbiddenException("Acesso negado"));

        mockMvc.perform(put("/api/investments/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado"));
    }

    // ─── DELETE /api/investments/{id} ─────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void deleteInvestment_authenticated_returns204() throws Exception {
        doNothing().when(investmentsService).deleteInvestment(1L, USER_EMAIL);

        mockMvc.perform(delete("/api/investments/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(investmentsService, times(1)).deleteInvestment(1L, USER_EMAIL);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void deleteInvestment_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Investimento não encontrado"))
                .when(investmentsService).deleteInvestment(99L, USER_EMAIL);

        mockMvc.perform(delete("/api/investments/99").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Investimento não encontrado"));
    }
}
