package com.igorAugusto.domus.domus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// ── Response para cada despesa ativa no mês ──────────────────
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMonthResponse {

    private Long outgoingId;
    private String description;
    private BigDecimal value;
    private String category;
    private String paymentType;
    private String frequency;
    private String startDate;
    private String yearMonth;   // mês de referência ex: "2026-04"
    private Boolean paid;       // status específico desse mês
}