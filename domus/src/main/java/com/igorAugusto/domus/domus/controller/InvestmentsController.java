package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.InvestmentsRequest;
import com.igorAugusto.domus.domus.dto.InvestmentsResponse;
import com.igorAugusto.domus.domus.service.InvestmentsService;
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

    @GetMapping
    public ResponseEntity<Page<InvestmentsResponse>> getAllInvestments(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable
    ) {
        return ResponseEntity.ok(
                investmentsService.getAllInvestments(userDetails.getUsername(), pageable)
        );
    }

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
