package com.igorAugusto.domus.domus.config;

import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class DemoUserInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Demo
    @Value("${app.users.demo.name}")
    private String demoName;

    @Value("${app.users.demo.email}")
    private String demoEmail;

    @Value("${app.users.demo.password}")
    private String demoPassword;

    // Admin (sua conta)
    @Value("${app.users.admin.name}")
    private String adminName;

    @Value("${app.users.admin.email}")
    private String adminEmail;

    @Value("${app.users.admin.password}")
    private String adminPassword;

    @PostConstruct
    public void createUsers() {
        createUserIfNotExists(demoName, demoEmail, demoPassword);
        createUserIfNotExists(adminName, adminEmail, adminPassword);
    }

    private void createUserIfNotExists(String name, String email, String password) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }
    }
}