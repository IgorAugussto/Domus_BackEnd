package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.MonthlyProjectionResponse;
import com.igorAugusto.domus.domus.entity.Income;
import com.igorAugusto.domus.domus.entity.Outgoing;
import com.igorAugusto.domus.domus.repository.IncomeRepository;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardProjectionService {

    private final IncomeRepository incomeRepository;
    private final OutgoingRepository outgoingRepository;

    public List<MonthlyProjectionResponse> projectNext12Months(Long userId) {

        Map<YearMonth, MonthlyProjectionResponse> projection = new LinkedHashMap<>();

        YearMonth startMonth = YearMonth.now();


        // 1️⃣ cria os 12 meses
        for (int i = 0; i < 12; i++) {
            YearMonth ym = YearMonth.now().plusMonths(i);
            projection.put(
                ym,
                new MonthlyProjectionResponse(
                        ym.toString(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )
            );
        }

        // 2️⃣ incomes
        List<Income> incomes = incomeRepository.findAllByUserId(userId);

        for (Income income : incomes) {

            YearMonth incomeStart = YearMonth.from(income.getStartDate());
            boolean recurring = Boolean.TRUE.equals(income.getRecurring());

            YearMonth incomeEnd;

            // income único
            if (recurring) {
                // salário: projeta 12 meses a partir do início do gráfico
                incomeEnd = startMonth.plusMonths(11);
            } else {
                // income único
                incomeEnd = incomeStart;
            }

            for (YearMonth ym : projection.keySet()) {
                if (!ym.isBefore(incomeStart) && !ym.isAfter(incomeEnd)) {
                    MonthlyProjectionResponse item = projection.get(ym);
                    item.setIncome(item.getIncome().add(income.getValue()));
                }
            }

        }

        // 3️⃣ outgoings
        List<Outgoing> outgoings = outgoingRepository.findAllByUserId(userId);

        for (Outgoing outgoing : outgoings) {

            YearMonth start = YearMonth.from(outgoing.getStartDate());
            YearMonth end = start.plusMonths(outgoing.getDurationInMonths() - 1);

            for (YearMonth ym : projection.keySet()) {
                if (!ym.isBefore(start) && !ym.isAfter(end)) {
                    MonthlyProjectionResponse item = projection.get(ym);
                    item.setExpenses(item.getExpenses().add(outgoing.getValue()));
                }
            }
        }

        return new ArrayList<>(projection.values());
    }

    /**
     * ============================
     * TAB MENSAL — 30 DIAS
     * ============================
     */
    public List<MonthlyProjectionResponse> projectCurrentMonthDays(Long userId) {

    Map<LocalDate, MonthlyProjectionResponse> projection = new LinkedHashMap<>();

    YearMonth currentMonth = YearMonth.now();

    // 1️⃣ Cria todos os dias do mês atual
    for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
        LocalDate date = currentMonth.atDay(day);
        projection.put(
            date,
            new MonthlyProjectionResponse(
                date.toString(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
            )
        );
    }

    // 2️⃣ Aplica INCOMES
    List<Income> incomes = incomeRepository.findAllByUserId(userId);

    for (Income income : incomes) {

        boolean isRecurringMonthly =
                Boolean.TRUE.equals(income.getRecurring()) &&
                "Monthly".equalsIgnoreCase(income.getFrequency());

        for (LocalDate date : projection.keySet()) {

            boolean isActiveInThisDay =
                    !date.isBefore(income.getStartDate()) &&
                    (income.getEndDate() == null || !date.isAfter(income.getEndDate()));

            if (!isActiveInThisDay) continue;

            if (isRecurringMonthly) {
                MonthlyProjectionResponse item = projection.get(date);
                item.setIncome(item.getIncome().add(income.getValue()));
            }
        }
    }

    // 3️⃣ Aplica OUTGOINGS (mesma lógica)
    List<Outgoing> outgoings = outgoingRepository.findAllByUserId(userId);

    for (Outgoing outgoing : outgoings) {

        LocalDate start = outgoing.getStartDate();
        LocalDate end = start.plusMonths(outgoing.getDurationInMonths());

        for (LocalDate date : projection.keySet()) {
            if (!date.isBefore(start) && !date.isAfter(end)) {
                MonthlyProjectionResponse item = projection.get(date);
                item.setExpenses(item.getExpenses().add(outgoing.getValue()));
            }
        }
    }

    return new ArrayList<>(projection.values());
}


}

