package com.igorAugusto.domus.domus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatementImportResponse {

    private int total;           // total de transações recebidas do Python
    private int saved;           // quantas foram salvas com sucesso
    private int skipped;         // quantas já existiam no banco (deduplicadas)
    private List<String> errors; // descrição dos erros se houver
}