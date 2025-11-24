package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.OutgoingRequest;
import com.igorAugusto.domus.domus.dto.OutgoingResponse;
import com.igorAugusto.domus.domus.service.OutgoingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/outgoing")
@RequiredArgsConstructor
public class OutgoingController {

    private final OutgoingService outgoingService;

    // POST /api/outgoing - Criar despesa
    @PostMapping
    public ResponseEntity<OutgoingResponse> createOutgoing(
            @RequestBody @Valid OutgoingRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                outgoingService.createOutgoing(request, userDetails.getUsername())
        );
    }

    // GET /api/outgoing - Listar despesas
    @GetMapping
    public ResponseEntity<List<OutgoingResponse>> getAllOutgoings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                outgoingService.getAllOutgoings(userDetails.getUsername())
        );
    }

    // GET /api/outgoing/total - Total de despesas
    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getTotalOutgoing(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                outgoingService.getTotalOutgoing(userDetails.getUsername())
        );
    }
}

