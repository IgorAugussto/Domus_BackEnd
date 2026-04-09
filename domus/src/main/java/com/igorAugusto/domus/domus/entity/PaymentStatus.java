package com.igorAugusto.domus.domus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_status",
        uniqueConstraints = {
                // Garante que só existe um registro por despesa + mês
                @UniqueConstraint(columnNames = {"outgoing_id", "year_month"})
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referência à despesa
    @ManyToOne
    @JoinColumn(name = "outgoing_id", nullable = false)
    private Outgoing outgoing;

    // Mês de referência no formato "2026-04"
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    // Status de pagamento para esse mês
    @Column(nullable = false)
    @Builder.Default
    private Boolean paid = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}