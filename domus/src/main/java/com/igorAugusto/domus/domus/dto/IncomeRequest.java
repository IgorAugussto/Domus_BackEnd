package com.igorAugusto.domus.domus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomeRequest {

    @NotNull(message = "Valor é obrigatório")
    private BigDecimal value;

    @NotBlank(message = "Descrição é obrigatória")
    private String description;

    @NotNull(message = "Data é obrigatória")
    private LocalDate date;
    
    private String frequency;


    private String category;  // Opcional
}
