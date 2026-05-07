package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.AuthResponse;
import com.igorAugusto.domus.domus.dto.LoginRequest;
import com.igorAugusto.domus.domus.dto.RegisterRequest;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@domus.app")
                .name("Igor Augusto")
                .password("encodedPassword")
                .build();
    }

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    void register_success_returnsTokenAndUserData() {
        RegisterRequest request = new RegisterRequest("test@domus.app", "plainPassword", "Igor Augusto");

        when(userRepository.existsByEmail("test@domus.app")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token-gerado");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt-token-gerado");
        assertThat(response.getEmail()).isEqualTo("test@domus.app");
        assertThat(response.getNome()).isEqualTo("Igor Augusto");
        assertThat(response.getTipo()).isEqualTo("Bearer");
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("plainPassword");
    }

    @Test
    void register_emailAlreadyExists_throwsRuntimeException() {
        RegisterRequest request = new RegisterRequest("test@domus.app", "password", "Igor");

        when(userRepository.existsByEmail("test@domus.app")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email já cadastrado!");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokenAndUserData() {
        LoginRequest request = new LoginRequest("test@domus.app", "plainPassword");

        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token-gerado");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token-gerado");
        assertThat(response.getEmail()).isEqualTo("test@domus.app");
        assertThat(response.getTipo()).isEqualTo("Bearer");
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_invalidCredentials_throwsBadCredentialsException() {
        LoginRequest request = new LoginRequest("test@domus.app", "senhaErrada");

        // AuthenticationManager lança BadCredentialsException para credenciais inválidas
        doThrow(new BadCredentialsException("Credenciais inválidas"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_userNotFoundAfterAuthentication_throwsRuntimeException() {
        // Caso raro: authenticate passa, mas o usuário não existe no banco
        LoginRequest request = new LoginRequest("ghost@domus.app", "password");

        when(userRepository.findByEmail("ghost@domus.app")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado");

        verify(jwtService, never()).generateToken(any());
    }
}
