package com.igorAugusto.domus.domus.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.igorAugusto.domus.domus.enums.Frequency;

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
    private BigDecimal value;  //  Valor da despesa

    @Column(nullable = false)
    private String description;  // Ex: "Conta de Luz"

    @Column(nullable = false)
    private LocalDate startDate;  // Data que gastou

    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = "duration_in_months", nullable = false)
    private Integer durationInMonths;

    @Column
    private String category;  // Ex: "Alimentação", "Transporte", "Lazer"

    @Column(name = "payment_type")          //  adicionado
    private String paymentType;

    @Column(nullable = false)               //  adicionado
    @Builder.Default
    private Boolean paid = false;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  //  Cada despesa pertence a um usuário

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Chave de deduplicação para importações (null para lançamentos manuais)
    @Column(name = "import_hash")
    private String importHash;

    // Data original da transação no arquivo do banco (null para lançamentos manuais)
    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @ManyToOne
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
