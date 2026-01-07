package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.DashboardMonthlySummaryResponse;
import com.igorAugusto.domus.domus.dto.DashboardSummaryResponse;
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
        private final OutgoingRepository outgoingRepository;
        private final InvestmentsRepository investmentsRepository;
        private final UserRepository userRepository;

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

        public DashboardMonthlySummaryResponse getMonthlySummary(Long userId, String monthStr) {
                YearMonth targetMonth = YearMonth.parse(monthStr);
                int ym = targetMonth.getYear() * 100 + targetMonth.getMonthValue();

                YearMonth previousMonth = targetMonth.minusMonths(1);
                int previousYm = previousMonth.getYear() * 100 + previousMonth.getMonthValue();

                // Income total acumulado até o mês atual (inclusive)
                BigDecimal totalIncomeUntilMonth = defaultZero(
                                incomeRepository.sumIncomeUntilMonth(userId, ym));

                // Income total acumulado até o mês anterior
                BigDecimal totalIncomeUntilPreviousMonth = defaultZero(
                                incomeRepository.sumIncomeUntilMonth(userId, previousYm));

                // Income apenas do mês atual
                BigDecimal incomeOfCurrentMonth = totalIncomeUntilMonth.subtract(totalIncomeUntilPreviousMonth);

                // Despesas acumuladas até o mês anterior
                BigDecimal totalExpensesUntilPreviousMonth = defaultZero(
                                outgoingRepository.sumOutgoingsUntilMonth(userId, previousYm));

                // Net worth (saldo) até o final do mês anterior
                BigDecimal netWorthUntilPreviousMonth = totalIncomeUntilPreviousMonth
                                .subtract(totalExpensesUntilPreviousMonth);

                // Despesas apenas do mês atual
                BigDecimal expensesOfMonth = defaultZero(
                                outgoingRepository.sumOutgoingsByExactMonth(userId, ym));

                // Investimentos apenas do mês atual
                BigDecimal investmentsOfMonth = defaultZero(
                                investmentsRepository.sumInvestmentsByExactMonth(userId, ym));

                // Income exibido na tela = income do mês atual + saldo acumulado do mês
                // anterior
                BigDecimal displayedIncome = incomeOfCurrentMonth.add(netWorthUntilPreviousMonth);

                // Net worth final do mês atual
                BigDecimal netWorth = displayedIncome.subtract(expensesOfMonth);

                // Savings rate (poupança em relação ao "income" exibido)
                BigDecimal savingsRate = displayedIncome.compareTo(BigDecimal.ZERO) > 0
                                ? netWorth.divide(displayedIncome, 4, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                : BigDecimal.ZERO;

                return new DashboardMonthlySummaryResponse(
                                targetMonth.toString(),
                                displayedIncome,
                                expensesOfMonth,
                                investmentsOfMonth,
                                netWorth,
                                savingsRate);
        }

}
