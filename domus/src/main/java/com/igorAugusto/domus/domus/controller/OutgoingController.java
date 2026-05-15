package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.OutgoingRequest;
import com.igorAugusto.domus.domus.dto.OutgoingResponse;
import com.igorAugusto.domus.domus.service.OutgoingService;

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
@RequestMapping("/api/costs")
@RequiredArgsConstructor
public class OutgoingController {

    private final OutgoingService outgoingService;

    @PostMapping
    public ResponseEntity<OutgoingResponse> createOutgoing(
            @RequestBody @Valid OutgoingRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                outgoingService.createOutgoing(request, userDetails.getUsername())
        );
    }

    @GetMapping
    public ResponseEntity<Page<OutgoingResponse>> getAllOutgoings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable
    ) {
        return ResponseEntity.ok(
                outgoingService.getAllOutgoings(userDetails.getUsername(), pageable)
        );
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getTotalOutgoing(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                outgoingService.getTotalOutgoing(userDetails.getUsername())
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<OutgoingResponse> updateOutgoing(
            @PathVariable Long id,
            @RequestBody @Valid OutgoingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                outgoingService.updateOutgoing(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOutgoing(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        outgoingService.deleteOutgoing(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
