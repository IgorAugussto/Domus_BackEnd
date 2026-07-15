package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.ImportJobStatusResponse;
import com.igorAugusto.domus.domus.dto.StatementJobResponse;
import com.igorAugusto.domus.domus.service.StatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/statement")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;

    /**
     * POST /api/statement/import
     *
     * Recebe o arquivo CSV/OFX/PDF e a data de vencimento.
     * Persiste o arquivo no banco, publica no RabbitMQ e responde IMEDIATAMENTE
     * com 202 Accepted + { jobId }.
     *
     * O cliente usa o jobId para fazer polling em /api/statement/status/{jobId}.
     */
    @PostMapping("/import")
    public ResponseEntity<StatementJobResponse> importStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dueDate") String dueDate,
            @RequestParam(value = "creditCardId", required = false) Long creditCardId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String filename = file.getOriginalFilename();
        if (filename == null ||
                (!filename.toLowerCase().endsWith(".csv") &&
                        !filename.toLowerCase().endsWith(".ofx") &&
                        !filename.toLowerCase().endsWith(".pdf"))) {
            return ResponseEntity.badRequest().build();
        }

        if (dueDate == null || dueDate.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String jobId = statementService.enqueueImport(file, dueDate, creditCardId, userDetails.getUsername());

        // 202 Accepted: requisição aceita, processamento ocorre em background
        return ResponseEntity.accepted().body(new StatementJobResponse(jobId));
    }

    /**
     * GET /api/statement/status/{jobId}
     *
     * Polling do frontend para verificar o andamento do job.
     * Retorna o status atual e, quando DONE, o resultado completo.
     *
     * Segurança: validado que o jobId pertence ao usuário autenticado.
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<ImportJobStatusResponse> getImportStatus(
            @PathVariable String jobId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ImportJobStatusResponse status = statementService.getJobStatus(jobId, userDetails.getUsername());
        return ResponseEntity.ok(status);
    }
}
