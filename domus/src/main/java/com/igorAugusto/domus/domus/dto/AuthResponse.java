package com.igorAugusto.domus.domus.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    @JsonIgnore
    private String token;
    private String tipo;
    private String email;
    private String nome;
}
