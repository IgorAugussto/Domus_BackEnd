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

    public MonthlySummaryResponse getMonthlySummary(Long userId, String monthStr) { // ex: "2026-02"
        YearMonth selectedMonth = YearMonth.parse(monthStr); // 2026-02
        int ym = selectedMonth.getYear() * 100 + selectedMonth.getMonthValue(); // 202602

        // 1. Income apenas do mês selecionado (projetado, incluindo recorrentes)
        BigDecimal monthlyIncome = defaultZero(
                incomeRepository.sumIncomeByExactMonth(userId, ym)
        );

        // 2. Expenses apenas do mês selecionado
        BigDecimal monthlyExpenses = defaultZero(
                costRepository.sumExpensesByExactMonth(userId, ym)
        );

        // 3. Net Worth acumulado ATÉ O MÊS ANTERIOR ao selecionado
        //    Ou seja: todo superávit (income - expenses) de todos os meses anteriores
        YearMonth previousMonth = selectedMonth.minusMonths(1);
        int previousYm = previousMonth.getYear() * 100 + previousMonth.getMonthValue();

        BigDecimal netWorthUntilPreviousMonth = calculateCumulativeNetUntilMonth(userId, previousYm);
        // Essa função você já tem partes dela: soma (incomes - expenses) até previousYm

        // 4. Cálculo final dos campos que vão para o frontend
        BigDecimal displayedIncome = monthlyIncome.add(netWorthUntilPreviousMonth);
        BigDecimal displayedNetWorth = displayedIncome.subtract(monthlyExpenses);
        BigDecimal savingsRate = displayedIncome.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : displayedNetWorth.divide(displayedIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return new MonthlySummaryResponse(
                displayedIncome,          // ← Esse é o "Income" que aparece na tela
                monthlyExpenses,          // ← Expenses normais do mês
                displayedNetWorth,        // ← Net Worth até o final do mês selecionado
                savingsRate
                // outros campos...
        );
    }

    private BigDecimal calculateCumulativeNetUntilMonth(Long userId, int yearMonth) {
        BigDecimal totalIncome = defaultZero(incomeRepository.sumIncomeUntilMonth(userId, yearMonth));
        BigDecimal totalExpenses = defaultZero(costRepository.sumExpensesUntilMonth(userId, yearMonth));
        return totalIncome.subtract(totalExpenses);
    }




    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
