package com.igorAugusto.domus.domus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igorAugusto.domus.domus.config.SecurityConfig;
import com.igorAugusto.domus.domus.dto.AuthResponse;
import com.igorAugusto.domus.domus.dto.LoginRequest;
import com.igorAugusto.domus.domus.dto.RegisterRequest;
import com.igorAugusto.domus.domus.service.AuthService;
import com.igorAugusto.domus.domus.service.JwtService;
import com.igorAugusto.domus.domus.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    // ─── POST /api/auth/login ─────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("test@domus.app", "senha123");
        AuthResponse response = new AuthResponse("jwt-token", "Bearer", "test@domus.app", "Igor Augusto");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.email").value("test@domus.app"))
                .andExpect(jsonPath("$.nome").value("Igor Augusto"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void login_serviceThrowsRuntimeException_returns500() throws Exception {
        LoginRequest request = new LoginRequest("test@domus.app", "senhaErrada");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Usuário não encontrado"));

        // GlobalExceptionHandler mapeia Exception genérica para 500
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ─── POST /api/auth/register ──────────────────────────────────────────────

    @Test
    void register_demoModeEnabled_returns403() throws Exception {
        // application.yml define app.demo-mode=true por padrão
        RegisterRequest request = new RegisterRequest("novo@domus.app", "senha123", "Novo User");

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("demo mode")));

        // authService.register jamais deve ser chamado em modo demo
        verify(authService, never()).register(any());
    }

    // ─── GET /api/auth/me ─────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getProfile_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(content().string("Você está autenticado!"));
    }

    @Test
    void getProfile_unauthenticated_returns200_routeIsPublicUnderAuthPath() throws Exception {
        // /api/auth/** é permitAll() na SecurityConfig, incluindo /api/auth/me
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk());
    }
}
