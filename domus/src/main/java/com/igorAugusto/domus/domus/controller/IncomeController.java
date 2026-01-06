package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.IncomeRequest;
import com.igorAugusto.domus.domus.dto.IncomeResponse;
import com.igorAugusto.domus.domus.service.IncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/income")
@RequiredArgsConstructor
public class IncomeController {

        private final IncomeService incomeService;

        // POST /api/income - Criar receita
        @PostMapping
        public ResponseEntity<IncomeResponse> createIncome(
                @RequestBody @Valid IncomeRequest request,
                @AuthenticationPrincipal UserDetails userDetails) {
                return ResponseEntity.ok(
                        incomeService.createIncome(request, userDetails.getUsername()));
        }

        // GET /api/income - Listar receitas
        @GetMapping
        public ResponseEntity<List<IncomeResponse>> getAllIncomes(
                @AuthenticationPrincipal UserDetails userDetails) {
                return ResponseEntity.ok(
                        incomeService.getAllIncomes(userDetails.getUsername()));
        }

        // GET /api/income/total - Total de receitas
        @GetMapping("/total")
        public ResponseEntity<BigDecimal> getTotalIncome(
                @AuthenticationPrincipal UserDetails userDetails) {
                return ResponseEntity.ok(
                        incomeService.getTotalIncome(userDetails.getUsername()));
        }

        @PutMapping("/{id}")
        public ResponseEntity<IncomeResponse> updateIncome(
                @PathVariable Long id,
                @RequestBody @Valid IncomeRequest request,
                @AuthenticationPrincipal UserDetails userDetails) {
                return ResponseEntity.ok(
                        incomeService.updateIncome(id, request, userDetails.getUsername()));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteIncome(
                @PathVariable Long id,
                @AuthenticationPrincipal UserDetails userDetails) {
                incomeService.deleteIncome(id, userDetails.getUsername());
                return ResponseEntity.noContent().build();
        }

}
