package com.igorAugusto.domus.domus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditCardRequest {

    @NotBlank(message = "Bandeira é obrigatória")
    private String brand;

    @NotBlank(message = "Últimos 4 dígitos são obrigatórios")
    @Pattern(regexp = "\\d{4}", message = "Informe exatamente 4 dígitos numéricos")
    private String lastFourDigits;

    private String nickname;
}
