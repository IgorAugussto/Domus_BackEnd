package com.igorAugusto.domus.domus.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvestmentsRequest {

    @NotNull(message = "Valor é obrigatório")
    private BigDecimal value;
    private String typeInvestments;
    @NotNull(message = "Data de criação é obrigatória")
    private LocalDate date;

}
