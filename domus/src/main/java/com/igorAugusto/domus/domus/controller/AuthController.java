package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.AuthResponse;
import com.igorAugusto.domus.domus.dto.LoginRequest;
import com.igorAugusto.domus.domus.dto.RegisterRequest;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.demo-mode}")
    private boolean demoMode;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpiration;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildJwtCookie(authResponse.getToken()).toString())
                .body(authResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (demoMode) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Registration disabled. Application running in demo mode.");
        }

        AuthResponse authResponse = authService.register(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildJwtCookie(authResponse.getToken()).toString())
                .body(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        return ResponseEntity.ok(new AuthResponse(null, null, user.getEmail(), user.getName()));
    }

    private ResponseCookie buildJwtCookie(String token) {
        return ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(jwtExpiration))
                .sameSite("None")
                .build();
    }
}
