package com.igorAugusto.domus.domus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Retorna uma lista de permissões/roles. Para simplicidade, apenas um role fixo.
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return this.email; // Usa o email como nome de usuário
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Conta nunca expira (simplificado)
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Conta nunca está bloqueada (simplificado)
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Credenciais nunca expiram (simplificado)
    }

    @Override
    public boolean isEnabled() {
        return true; // Conta sempre habilitada (simplificado)
    }
}