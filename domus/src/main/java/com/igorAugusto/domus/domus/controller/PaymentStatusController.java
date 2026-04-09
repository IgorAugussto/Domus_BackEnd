package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.PaymentMonthResponse;
import com.igorAugusto.domus.domus.service.PaymentStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/payment-status")
@RequiredArgsConstructor
public class PaymentStatusController {

    private final PaymentStatusService paymentStatusService;

    /**
     * GET /api/payment-status?yearMonth=2026-04
     * Retorna todas as despesas ativas no mês com seus status.
     * Se não informar yearMonth, usa o mês atual.
     */
    @GetMapping
    public ResponseEntity<List<PaymentMonthResponse>> getPaymentsByMonth(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String yearMonth) {

        String targetMonth = (yearMonth != null && !yearMonth.isBlank())
                ? yearMonth
                : YearMonth.now().toString();

        List<PaymentMonthResponse> payments = paymentStatusService
                .getPaymentsByMonth(userDetails.getUsername(), targetMonth);

        return ResponseEntity.ok(payments);
    }

    /**
     * PUT /api/payment-status/{outgoingId}?yearMonth=2026-04&paid=true
     * Marca uma despesa como paga ou pendente em um mês específico.
     */
    @PutMapping("/{outgoingId}")
    public ResponseEntity<PaymentMonthResponse> togglePaymentStatus(
            @PathVariable Long outgoingId,
            @RequestParam String yearMonth,
            @RequestParam boolean paid,
            @AuthenticationPrincipal UserDetails userDetails) {

        PaymentMonthResponse response = paymentStatusService.togglePaymentStatus(
                userDetails.getUsername(),
                outgoingId,
                yearMonth,
                paid
        );

        return ResponseEntity.ok(response);
    }
}