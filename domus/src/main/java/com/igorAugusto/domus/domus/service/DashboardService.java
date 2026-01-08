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
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

        private final IncomeRepository incomeRepository;
        private final OutgoingRepository outgoingRepository;
        private final InvestmentsRepository investmentsRepository;
        private final UserRepository userRepository;
        private final DashboardProjectionService dashboardProjectionService;

        private BigDecimal defaultZero(BigDecimal value) {
                return value != null ? value : BigDecimal.ZERO;
        }

        public DashboardSummaryResponse getSummary(String email) {

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                BigDecimal totalIncome = defaultZero(incomeRepository.sumByUserId(user.getId()));

                BigDecimal totalCost = defaultZero(outgoingRepository.sumByUserId(user.getId()));

                BigDecimal totalInvestments = defaultZero(investmentsRepository.sumByUserId(user.getId()));

                // 🔥 GANHO REAL DE INVESTIMENTOS (expectedReturn)
                List<Investments> investments = investmentsRepository.findAllByUserId(user.getId());

                BigDecimal investmentGains = investments.stream()
                                .map(inv -> inv.getValue().multiply(
                                                BigDecimal.valueOf(inv.getExpectedReturn() / 100)))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal netIncome = totalIncome.subtract(totalCost);

                BigDecimal netWorth = netIncome.add(totalInvestments).add(investmentGains);

                BigDecimal savingsRate = BigDecimal.ZERO;
                if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
                        savingsRate = netIncome
                                        .divide(totalIncome, 4, RoundingMode.HALF_UP)
                                        .multiply(new BigDecimal("100"));
                }

                return new DashboardSummaryResponse(
                                investmentGains,
                                netWorth,
                                savingsRate);
        }

        public DashboardMonthlySummaryResponse getMonthlySummary(
                Long userId,
                String monthStr
        ) {
                YearMonth targetMonth = YearMonth.parse(monthStr);

                List<MonthlyProjectionResponse> projection =
                        dashboardProjectionService.projectNext12Months(userId);

                // 🔥 ordena por mês (garantia)
                projection.sort(Comparator.comparing(MonthlyProjectionResponse::getMonth));

                BigDecimal accumulatedBalance = BigDecimal.ZERO;

                MonthlyProjectionResponse current = null;

                for (MonthlyProjectionResponse month : projection) {

                        // saldo entra antes do income do mês
                        BigDecimal displayedIncome =
                                accumulatedBalance.add(month.getIncome());

                        BigDecimal netWorth =
                                displayedIncome.subtract(month.getExpenses());

                        // atualiza saldo acumulado
                        accumulatedBalance = netWorth;

                        if (month.getMonth().equals(targetMonth.toString())) {
                                current = new MonthlyProjectionResponse(
                                        month.getMonth(),
                                        displayedIncome,
                                        month.getExpenses(),
                                        month.getInvestments()
                                );
                                break;
                        }
                }

                if (current == null) {
                        throw new RuntimeException("Mês não encontrado na projeção");
                }

                BigDecimal savingsRate =
                        current.getIncome().compareTo(BigDecimal.ZERO) > 0
                                ? accumulatedBalance
                                .divide(current.getIncome(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO;

                return new DashboardMonthlySummaryResponse(
                        current.getMonth(),
                        current.getIncome(),
                        current.getExpenses(),
                        current.getInvestments(),
                        accumulatedBalance,
                        savingsRate
                );
        }



}
