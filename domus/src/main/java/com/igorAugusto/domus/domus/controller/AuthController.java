package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.config.JwtTokenProvider;
import com.igorAugusto.domus.domus.dto.LoginRequest;
import com.igorAugusto.domus.domus.dto.LoginResponse;
import com.igorAugusto.domus.domus.dto.RegisterRequest;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authenticationManager, UserService userService,
                          PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Validated @RequestBody RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            LoginResponse response = new LoginResponse();
            response.setMessage("E-mail já existe");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());

        userService.saveUser(user);

        String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getId());
        LoginResponse response = new LoginResponse();
        response.setToken(jwt);
        response.setMessage("Usuário registrado com sucesso");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) userService.loadUserByUsername(request.getEmail());
        String jwt = jwtTokenProvider.generateToken(user.getEmail(), user.getId());

        LoginResponse response = new LoginResponse();
        response.setToken(jwt);
        response.setMessage("Login bem-sucedido");

        return ResponseEntity.ok(response);
    }
}
