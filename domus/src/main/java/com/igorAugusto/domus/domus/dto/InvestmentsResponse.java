package com.igorAugusto.domus.domus.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvestmentsResponse {
    
    private Long id;
    private BigDecimal value;  // ✅ Use BigDecimal para dinheiro!
    private String typeInvestments;
    private LocalDateTime createdAt;

}
