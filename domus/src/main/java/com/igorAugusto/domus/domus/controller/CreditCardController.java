package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.CreditCardRequest;
import com.igorAugusto.domus.domus.dto.CreditCardResponse;
import com.igorAugusto.domus.domus.service.CreditCardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit-cards")
@RequiredArgsConstructor
public class CreditCardController {

    private final CreditCardService creditCardService;

    @PostMapping
    public ResponseEntity<CreditCardResponse> createCreditCard(
            @RequestBody @Valid CreditCardRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                creditCardService.createCreditCard(request, userDetails.getUsername())
        );
    }

    @GetMapping
    public ResponseEntity<List<CreditCardResponse>> getActiveCards(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                creditCardService.getActiveCards(userDetails.getUsername())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateCreditCard(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        creditCardService.deactivateCreditCard(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
