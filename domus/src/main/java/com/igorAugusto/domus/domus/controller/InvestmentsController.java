package com.igorAugusto.domus.domus.controller;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import com.igorAugusto.domus.domus.dto.InvestmentsRequest;
import com.igorAugusto.domus.domus.dto.InvestmentsResponse;
import com.igorAugusto.domus.domus.service.InvestmentsService;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
public class InvestmentsController {

    private final InvestmentsService investmentsService;

    @PostMapping
    public ResponseEntity<InvestmentsResponse> createInvestments(
            @RequestBody @Valid InvestmentsRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                investmentsService.createInvestment(request, userDetails.getUsername())
        );
    }

    // GET /api/income - Listar receitas
    @GetMapping
    public ResponseEntity<List<InvestmentsResponse>> getAllInvestments(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                investmentsService.getAllInvestments(userDetails.getUsername())
        );
    }

    // GET /api/income/total - Total de receitas
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getTotalInvestments(
        @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                investmentsService.getTotalInvestments(userDetails.getUsername())
        );
    }

    @PutMapping("/{id}")
        public ResponseEntity<InvestmentsResponse> updateInvestment(
                        @PathVariable Long id,
                        @RequestBody @Valid InvestmentsRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {
                return ResponseEntity.ok(
                                investmentsService.updateInvestment(id, request, userDetails.getUsername()));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteInvestment(
                @PathVariable Long id,
                @AuthenticationPrincipal UserDetails userDetails) {
                        investmentsService.deleteInvestment(id, userDetails.getUsername());
                        return ResponseEntity.noContent().build();
        }
    
}
