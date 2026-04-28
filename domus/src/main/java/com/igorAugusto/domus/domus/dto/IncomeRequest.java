package com.igorAugusto.domus.domus.dto;

import com.igorAugusto.domus.domus.enums.Frequency;
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

    private String description;

    @NotNull(message = "Data é obrigatória")
    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean recurring;

    private Frequency frequency;

    private String category;
}
