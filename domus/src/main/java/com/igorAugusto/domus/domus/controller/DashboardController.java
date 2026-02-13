package com.igorAugusto.domus.domus.controller;

import com.igorAugusto.domus.domus.dto.DashboardMonthlySummaryResponse;
import com.igorAugusto.domus.domus.dto.DashboardSummaryResponse;
import com.igorAugusto.domus.domus.dto.MonthlyProjectionResponse;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.UserRepository;
import com.igorAugusto.domus.domus.service.DashboardProjectionService;
import com.igorAugusto.domus.domus.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.time.YearMonth;
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

    /**
     * ============================
     * 🔥 ENDPOINT ATUALIZADO - ANO FIXO
     * ============================
     * GET /api/dashboard/projection/year
     * GET /api/dashboard/projection/year?year=2026
     *
     * Retorna os 12 meses de um ano específico (Janeiro–Dezembro).
     * Se não informar o ano, retorna o ano atual.
     */
    @GetMapping("/projection/year")
    public List<MonthlyProjectionResponse> getYearProjection(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer year) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Se não passar ano, usa o ano atual
        Integer targetYear = (year != null) ? year : Year.now().getValue();

        return dashboardProjectionService.projectFixedYear(user.getId(), targetYear);
    }

    // ============================
    // GRÁFICO — ABA MENSAL (30 DIAS)
    // ============================
    @GetMapping("/projection/month")
    public List<MonthlyProjectionResponse> getMonthProjection(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return dashboardProjectionService.projectCurrentMonthDays(user.getId());
    }

    @GetMapping("/summary/monthly")
    public ResponseEntity<DashboardMonthlySummaryResponse> getMonthlySummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String month) {

        YearMonth resolvedMonth = YearMonth.now();

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        Long userId = user.getId();

        try {
            resolvedMonth = (month != null && !month.isBlank())
                    ? YearMonth.parse(month.trim())
                    : YearMonth.now();
        } catch (Exception e) {
            resolvedMonth = YearMonth.now();
        }

        String monthStr = resolvedMonth.toString();

        DashboardMonthlySummaryResponse response = dashboardService.getMonthlySummary(userId, monthStr);

        return ResponseEntity.ok(response);
    }
}