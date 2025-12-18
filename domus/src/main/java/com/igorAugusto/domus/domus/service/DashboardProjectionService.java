package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.MonthlyProjectionResponse;
import com.igorAugusto.domus.domus.entity.Income;
import com.igorAugusto.domus.domus.entity.Outgoing;
import com.igorAugusto.domus.domus.repository.IncomeRepository;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardProjectionService {

    private final IncomeRepository incomeRepository;
    private final OutgoingRepository outgoingRepository;

    // 👉👉👉 O CÓDIGO DA ETAPA 5 FICA AQUI 👇👇👇
    public List<MonthlyProjectionResponse> projectNext12Months(Long userId) {

        Map<YearMonth, MonthlyProjectionResponse> map = new LinkedHashMap<>();

        // cria os 12 meses
        for (int i = 0; i < 12; i++) {
            YearMonth ym = YearMonth.now().plusMonths(i);
            map.put(
                ym,
                new MonthlyProjectionResponse(
                        ym.toString(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )
            );
        }

        // aplica incomes
        List<Income> incomes = incomeRepository.findAllByUserId(userId);

        for (Income income : incomes) {

            YearMonth start = YearMonth.from(income.getStartDate());

            // income único
            if (!income.getRecurring()) {
                if (map.containsKey(start)) {
                    map.get(start).setIncome(
                        map.get(start).getIncome().add(income.getValue())
                    );
                }
                continue;
            }

            // income recorrente (salary)
            YearMonth end = YearMonth.from(income.getEndDate());

            for (YearMonth ym : map.keySet()) {
                if (!ym.isBefore(start) && !ym.isAfter(end)) {
                    map.get(ym).setIncome(
                        map.get(ym).getIncome().add(income.getValue())
                    );
                }
            }
        }

        // aplica outgoings (expenses)
        List<Outgoing> outgoings = outgoingRepository.findAllByUserId(userId);

        for (Outgoing out : outgoings) {
            YearMonth start = YearMonth.from(out.getStartDate());
            YearMonth end = start.plusMonths(out.getDurationInMonths() - 1);

            for (YearMonth ym : map.keySet()) {
                if (!ym.isBefore(start) && !ym.isAfter(end)) {
                    map.get(ym).setExpenses(
                        map.get(ym).getExpenses().add(out.getValue())
                    );
                }
            }
        }

        return new ArrayList<>(map.values());
    }
}
