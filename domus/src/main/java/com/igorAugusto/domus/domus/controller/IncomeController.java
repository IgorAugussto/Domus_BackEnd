package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.IncomeRequest;
import com.igorAugusto.domus.domus.dto.IncomeResponse;
import com.igorAugusto.domus.domus.service.IncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/income")
@RequiredArgsConstructor
public class IncomeController {

        private final IncomeService incomeService;

        @PostMapping
        public ResponseEntity<IncomeResponse> createIncome(
                @RequestBody @Valid IncomeRequest request,
                @AuthenticationPrincipal UserDetails userDetails) {
                return ResponseEntity.ok(
                        incomeService.createIncome(request, userDetails.getUsername()));
        }

        @GetMapping
        public ResponseEntity<Page<IncomeResponse>> getAllIncomes(
                @AuthenticationPrincipal UserDetails userDetails,
                @PageableDefault(size = 20, sort = "startDate") Pageable pageable) {
                return ResponseEntity.ok(
                        incomeService.getAllIncomes(userDetails.getUsername(), pageable));
        }

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
