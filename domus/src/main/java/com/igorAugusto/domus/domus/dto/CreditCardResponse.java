package com.igorAugusto.domus.domus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreditCardResponse {
    private Long id;
    private String brand;
    private String lastFourDigits;
    private String nickname;
    private Boolean active;
}
