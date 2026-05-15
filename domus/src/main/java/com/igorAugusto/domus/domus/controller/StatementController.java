package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.StatementImportResponse;
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
     * Recebe o arquivo CSV ou OFX e a data de vencimento do frontend,
     * envia para o Python processar e salva no banco como despesas.
     */
    @PostMapping("/import")
    public ResponseEntity<StatementImportResponse> importStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dueDate") String dueDate, // ✅ data de vencimento
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

        StatementImportResponse response = statementService.importStatement(
                file,
                dueDate,
                userDetails.getUsername()
        );

        return ResponseEntity.ok(response);
    }
}