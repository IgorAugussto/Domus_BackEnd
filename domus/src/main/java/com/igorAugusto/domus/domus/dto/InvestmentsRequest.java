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

    @NotNull(message = "Tipo de investimento é obrigatório")
    private String typeInvestments;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate startDate;

    @NotNull(message = "Data de término é obrigatória")
    private LocalDate endDate;

    private String description;

    @NotNull(message = "Retorno esperado é obrigatório")
    private double expectedReturn;



}
