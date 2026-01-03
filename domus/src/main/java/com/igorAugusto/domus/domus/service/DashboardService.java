package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.DashboardMonthlySummaryResponse;
import com.igorAugusto.domus.domus.dto.DashboardSummaryResponse;
import com.igorAugusto.domus.domus.dto.MonthlyProjectionResponse;
import com.igorAugusto.domus.domus.entity.Investments;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.IncomeRepository;
import com.igorAugusto.domus.domus.repository.InvestmentsRepository;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final IncomeRepository incomeRepository;
    private final OutgoingRepository costRepository;
    private final InvestmentsRepository investmentsRepository;
    private final UserRepository userRepository;
    private final DashboardProjectionService dashboardProjectionService;

    public DashboardSummaryResponse getSummary(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        BigDecimal totalIncome =
                defaultZero(incomeRepository.sumByUserId(user.getId()));

        BigDecimal totalCost =
                defaultZero(costRepository.sumByUserId(user.getId()));

        BigDecimal totalInvestments =
                defaultZero(investmentsRepository.sumByUserId(user.getId()));

        // 🔥 GANHO REAL DE INVESTIMENTOS (expectedReturn)
        List<Investments> investments =
                investmentsRepository.findAllByUserId(user.getId());

        BigDecimal investmentGains = investments.stream()
                .map(inv ->
                        inv.getValue().multiply(
                                BigDecimal.valueOf(inv.getExpectedReturn() / 100)
                        )
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netIncome = totalIncome.subtract(totalCost);

        BigDecimal netWorth =
                netIncome.add(totalInvestments).add(investmentGains);

        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netIncome
                    .divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return new DashboardSummaryResponse(
                investmentGains,
                netWorth,
                savingsRate
        );
    }

    public DashboardMonthlySummaryResponse getMonthlySummary(
            String email,
            YearMonth targetMonth
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Long userId = user.getId();

        /*
         * ============================
         * 1️⃣ BUSCA PROJEÇÃO DOS MESES
         * ============================
         */
        // Reaproveitamos a projeção já existente
        // (fonte única da verdade para gráfico + lógica mensal)
        List<MonthlyProjectionResponse> projections =
                dashboardProjectionService.projectNext12Months(userId);

        BigDecimal accumulatedNetWorth = BigDecimal.ZERO;

        BigDecimal monthIncome = BigDecimal.ZERO;
        BigDecimal monthExpenses = BigDecimal.ZERO;
        BigDecimal monthInvestments = BigDecimal.ZERO;

        for (MonthlyProjectionResponse item : projections) {

            YearMonth itemMonth = YearMonth.parse(item.getMonth());

            // 🔹 acumula mês a mês
            accumulatedNetWorth = accumulatedNetWorth
                    .add(item.getIncome())
                    .subtract(item.getExpenses());

            // 🔹 quando chegar no mês alvo
            if (itemMonth.equals(targetMonth)) {

                monthIncome = item.getIncome();
                monthExpenses = item.getExpenses();
                monthInvestments = item.getInvestments();

                break;
            }
        }

        /*
         * ============================
         * 2️⃣ SAVINGS RATE
         * ============================
         */
        BigDecimal savingsRate = BigDecimal.ZERO;

        if (monthIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = accumulatedNetWorth
                    .divide(monthIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return new DashboardMonthlySummaryResponse(
                targetMonth.toString(),
                monthIncome,
                monthExpenses,
                monthInvestments,
                accumulatedNetWorth,
                savingsRate
        );
    }



    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
