package com.igorAugusto.domus.domus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeDTO(@NotNull @Positive BigDecimal valor,
                        @NotBlank String descricao,
                        @NotNull LocalDate data)
{}
