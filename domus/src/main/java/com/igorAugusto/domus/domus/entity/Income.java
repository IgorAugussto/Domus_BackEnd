package com.igorAugusto.domus.domus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "income")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal value;  // ✅ Use BigDecimal para dinheiro!

    @Column(nullable = false)
    private String description;  // Ex: "Salário de Novembro"

    @Column(nullable = false)
    private LocalDate date;  // Data que recebeu

    @Column
    private String category;  // Ex: "Salário", "Freelance", "Investimento"

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // ✅ Cada receita pertence a um usuário

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
