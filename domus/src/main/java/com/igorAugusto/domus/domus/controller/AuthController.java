package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.AuthResponse;
import com.igorAugusto.domus.domus.dto.LoginRequest;
import com.igorAugusto.domus.domus.dto.RegisterRequest;
import com.igorAugusto.domus.domus.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.demo-mode}")
    private boolean demoMode;


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (demoMode) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Registration disabled. Application running in demo mode.");
        }

        return ResponseEntity.ok(authService.register(request));
    }


    // Endpoint protegido de exemplo
    @GetMapping("/me")
    public ResponseEntity<String> getProfile() {
        return ResponseEntity.ok("Você está autenticado!");
    }
}
