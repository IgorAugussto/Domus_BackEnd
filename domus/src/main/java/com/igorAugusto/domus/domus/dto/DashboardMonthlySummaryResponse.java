package com.igorAugusto.domus.domus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardMonthlySummaryResponse {

    private String month; // YYYY-MM

    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal investments;

    private BigDecimal netWorth;
    private BigDecimal savingsRate;
}
