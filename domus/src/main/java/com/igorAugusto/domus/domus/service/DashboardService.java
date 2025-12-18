package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.DashboardSummaryResponse;
import com.igorAugusto.domus.domus.entity.Investments;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
import com.igorAugusto.domus.domus.repository.IncomeRepository;
import com.igorAugusto.domus.domus.repository.InvestmentsRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final IncomeRepository incomeRepository;
    private final OutgoingRepository costRepository;
    private final InvestmentsRepository investmentsRepository;
    private final UserRepository userRepository;

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

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
