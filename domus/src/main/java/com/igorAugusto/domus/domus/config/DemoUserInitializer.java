package com.igorAugusto.domus.domus.config;

import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;


import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class DemoUserInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void createDemoUser() {
        if (!userRepository.existsByEmail("demo@domus.app")) {
            User user = new User();
            user.setName("Conta Demo");
            user.setEmail("demo@domus.app");
            user.setPassword(passwordEncoder.encode("demo123"));
            userRepository.save(user);
        }
    }
}

