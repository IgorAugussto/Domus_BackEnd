package com.igorAugusto.domus.domus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igorAugusto.domus.domus.dto.IncomeRequest;
import com.igorAugusto.domus.domus.dto.IncomeResponse;
import com.igorAugusto.domus.domus.enums.Frequency;
import com.igorAugusto.domus.domus.exception.BusinessException;
import com.igorAugusto.domus.domus.exception.ForbiddenException;
import com.igorAugusto.domus.domus.exception.ResourceNotFoundException;
import com.igorAugusto.domus.domus.service.IncomeService;
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

@WebMvcTest(value = IncomeController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class IncomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IncomeService incomeService;

    // Dependências da camada de segurança necessárias para o contexto de teste
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    private IncomeRequest validRequest;
    private IncomeResponse incomeResponse;

    private static final String USER_EMAIL = "test@domus.app";

    @BeforeEach
    void setUp() {
        validRequest = new IncomeRequest(
                BigDecimal.valueOf(5000.00),
                "Salário",
                LocalDate.of(2026, 1, 1),
                null,
                null,
                Frequency.MONTHLY,
                "Salário"
        );

        incomeResponse = new IncomeResponse(
                1L,
                BigDecimal.valueOf(5000.00),
                "Salário",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                true,
                "Salário",
                LocalDateTime.now(),
                Frequency.MONTHLY
        );
    }

    // ─── POST /api/income ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void createIncome_authenticatedWithValidBody_returns200WithResponse() throws Exception {
        when(incomeService.createIncome(any(IncomeRequest.class), eq(USER_EMAIL)))
                .thenReturn(incomeResponse);

        mockMvc.perform(post("/api/income")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Salário"))
                .andExpect(jsonPath("$.value").value(5000.00))
                .andExpect(jsonPath("$.recurring").value(true));

        verify(incomeService, times(1)).createIncome(any(IncomeRequest.class), eq(USER_EMAIL));
    }

    @Test
    void createIncome_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(post("/api/income")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().is4xxClientError());

        verify(incomeService, never()).createIncome(any(), any());
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void createIncome_missingRequiredValue_returns400() throws Exception {
        // value é @NotNull — corpo sem esse campo dispara MethodArgumentNotValidException
        String bodyWithoutValue = """
                {
                    "description": "Salário",
                    "startDate": "2026-01-01",
                    "frequency": "Monthly"
                }
                """;

        mockMvc.perform(post("/api/income")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutValue))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void createIncome_serviceThrowsBusinessException_returns400() throws Exception {
        when(incomeService.createIncome(any(IncomeRequest.class), eq(USER_EMAIL)))
                .thenThrow(new BusinessException("Data de início é obrigatória para receitas recorrentes"));

        mockMvc.perform(post("/api/income")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Data de início é obrigatória para receitas recorrentes"));
    }

    // ─── GET /api/income ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void getAllIncomes_authenticated_returns200WithPage() throws Exception {
        Page<IncomeResponse> page = new PageImpl<>(List.of(incomeResponse));

        when(incomeService.getAllIncomes(eq(USER_EMAIL), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/income"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].description").value("Salário"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllIncomes_unauthenticated_returns4xx() throws Exception {
        mockMvc.perform(get("/api/income"))
                .andExpect(status().is4xxClientError());

        verify(incomeService, never()).getAllIncomes(any(), any());
    }

    // ─── GET /api/income/total ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void getTotalIncome_authenticated_returns200WithTotal() throws Exception {
        when(incomeService.getTotalIncome(USER_EMAIL)).thenReturn(BigDecimal.valueOf(15000.00));

        mockMvc.perform(get("/api/income/total"))
                .andExpect(status().isOk())
                .andExpect(content().string("15000.0"));
    }

    // ─── PUT /api/income/{id} ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void updateIncome_authenticated_returns200WithUpdatedResponse() throws Exception {
        when(incomeService.updateIncome(eq(1L), any(IncomeRequest.class), eq(USER_EMAIL)))
                .thenReturn(incomeResponse);

        mockMvc.perform(put("/api/income/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void updateIncome_belongsToOtherUser_returns403() throws Exception {
        when(incomeService.updateIncome(eq(1L), any(IncomeRequest.class), eq(USER_EMAIL)))
                .thenThrow(new ForbiddenException("Acesso negado"));

        mockMvc.perform(put("/api/income/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado"));
    }

    // ─── DELETE /api/income/{id} ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void deleteIncome_authenticated_returns204() throws Exception {
        doNothing().when(incomeService).deleteIncome(1L, USER_EMAIL);

        mockMvc.perform(delete("/api/income/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(incomeService, times(1)).deleteIncome(1L, USER_EMAIL);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void deleteIncome_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Receita não encontrada"))
                .when(incomeService).deleteIncome(99L, USER_EMAIL);

        mockMvc.perform(delete("/api/income/99").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Receita não encontrada"));
    }
}
