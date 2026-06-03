package com.igorAugusto.domus.domus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta imediata ao POST /api/statement/import (HTTP 202 Accepted).
 * O cliente usa o jobId para fazer polling em GET /api/statement/status/{jobId}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatementJobResponse {
    private String jobId;
}
