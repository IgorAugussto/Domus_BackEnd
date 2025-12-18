package com.igorAugusto.domus.domus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomeResponse {
    private Long id;
    private BigDecimal value;
    private String description;
    private LocalDate date;
    private String category;
    private LocalDateTime createdAt;
    private String frequency;
}
