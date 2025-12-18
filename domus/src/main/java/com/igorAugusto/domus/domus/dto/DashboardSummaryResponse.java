package com.igorAugusto.domus.domus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryResponse {

    private BigDecimal investmentGains;
    private BigDecimal netWorth;
    private BigDecimal savingsRate;
}
