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
@Table(name = "outgoing")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Outgoing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal value;  // ✅ Valor da despesa

    @Column(nullable = false)
    private String description;  // Ex: "Conta de Luz"

    @Column(nullable = false)
    private LocalDate date;  // Data que gastou

    @Column(nullable = false)
    private String frequency;

    @Column
    private String category;  // Ex: "Alimentação", "Transporte", "Lazer"

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // ✅ Cada despesa pertence a um usuário

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
