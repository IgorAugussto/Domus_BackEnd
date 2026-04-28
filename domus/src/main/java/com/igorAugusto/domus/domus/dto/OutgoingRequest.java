package com.igorAugusto.domus.domus.dto;

import com.igorAugusto.domus.domus.enums.Frequency;
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
public class OutgoingRequest {

    @NotNull(message = "Valor é obrigatório")
    private BigDecimal value;

    @NotBlank(message = "Descrição é obrigatória")
    private String description;

    @NotNull(message = "Data é obrigatória")
    private LocalDate startDate;

    private Integer durationInMonths;

    private Frequency frequency;

    private String category;

    private String paymentType;

    private Boolean paid;
}
