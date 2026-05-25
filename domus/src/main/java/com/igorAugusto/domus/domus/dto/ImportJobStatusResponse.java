package com.igorAugusto.domus.domus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta do polling: GET /api/statement/status/{jobId}
 *
 * status: "PENDING" | "PROCESSING" | "DONE" | "ERROR"
 * result: preenchido apenas quando status == "DONE"
 *         (total, saved, skipped, errors)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportJobStatusResponse {
    private String status;
    private StatementImportResponse result; // null se PENDING/PROCESSING/ERROR
}
