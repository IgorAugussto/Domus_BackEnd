package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.DashboardSummaryResponse;
import com.igorAugusto.domus.domus.dto.MonthlyProjectionResponse;
import com.igorAugusto.domus.domus.service.DashboardProjectionService;
import com.igorAugusto.domus.domus.service.DashboardService;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.UserRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardProjectionService dashboardProjectionService;
    private final UserRepository userRepository;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(@AuthenticationPrincipal UserDetails userDetails) {
        return dashboardService.getSummary(userDetails.getUsername());
    }

    // 🔥 NOVO ENDPOINT — PROJEÇÃO DO GRÁFICO (12 MESES)
    @GetMapping("/projection/year")
    public List<MonthlyProjectionResponse> getYearProjection(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return dashboardProjectionService.projectNext12Months(user.getId());
    }

        // ============================
    // GRÁFICO — ABA MENSAL (30 DIAS)
    // ============================
    @GetMapping("/projection/month")
    public List<MonthlyProjectionResponse> getMonthProjection(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = userRepository.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return dashboardProjectionService.projectCurrentMonthDays(user.getId());
    }


}
