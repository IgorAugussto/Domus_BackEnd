package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.MonthlyProjectionResponse;
import com.igorAugusto.domus.domus.entity.Income;
import com.igorAugusto.domus.domus.entity.Investments;
import com.igorAugusto.domus.domus.entity.Outgoing;
import com.igorAugusto.domus.domus.repository.IncomeRepository;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
import com.igorAugusto.domus.domus.repository.InvestmentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardProjectionService {

    private final IncomeRepository incomeRepository;
    private final OutgoingRepository outgoingRepository;
    private final InvestmentsRepository investmentsRepository;

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
                            BigDecimal.ZERO));
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

        /*
         * ============================
         * 4️⃣ INVESTMENTS (🔥 PARTE QUE FALTAVA 🔥)
         * ============================
         */
        List<Investments> investments = investmentsRepository.findAllByUserId(userId);

        for (Investments inv : investments) {

            BigDecimal initialValue = inv.getValue();

            BigDecimal monthlyRate = BigDecimal
                    .valueOf(inv.getExpectedReturn())
                    .divide(BigDecimal.valueOf(100), MathContext.DECIMAL64)
                    .divide(BigDecimal.valueOf(12), MathContext.DECIMAL64);

            YearMonth investStart = YearMonth.from(inv.getStartDate());
            YearMonth investEnd = YearMonth.from(inv.getEndDate());

            for (YearMonth ym : projection.keySet()) {

                if (ym.isBefore(investStart) || ym.isAfter(investEnd)) {
                    continue;
                }

                long monthsPassed = ChronoUnit.MONTHS.between(investStart, ym);

                BigDecimal growthFactor = BigDecimal.ONE
                        .add(monthlyRate)
                        .pow((int) monthsPassed, MathContext.DECIMAL64);

                BigDecimal currentValue = initialValue.multiply(growthFactor, MathContext.DECIMAL64);

                MonthlyProjectionResponse item = projection.get(ym);
                item.setInvestments(item.getInvestments().add(currentValue));
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
                            BigDecimal.ZERO));
        }

        // 2️⃣ Aplica INCOMES
        List<Income> incomes = incomeRepository.findAllByUserId(userId);

    for (Income income : incomes) {

        for (LocalDate date : projection.keySet()) {

            boolean isActive =
                !date.isBefore(income.getStartDate()) &&
                (income.getEndDate() == null || !date.isAfter(income.getEndDate()));

            if (!isActive) continue;

            MonthlyProjectionResponse item = projection.get(date);

            // ✅ CASO 1: INCOME RECORRENTE MENSAL
            if (Boolean.TRUE.equals(income.getRecurring())
                && "Monthly".equalsIgnoreCase(income.getFrequency())) {

                item.setIncome(
                    item.getIncome().add(income.getValue())
                );
            }

            // ✅ CASO 2: INCOME NÃO RECORRENTE (ONE-TIME, BONUS, GIFT, ETC)
            if (!Boolean.TRUE.equals(income.getRecurring())
                && date.equals(income.getStartDate())) {

                item.setIncome(
                    item.getIncome().add(income.getValue())
                );
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

        // 4️⃣ INVESTIMENTOS (🔥 PARTE NOVA 🔥)
        List<Investments> investments = investmentsRepository.findAllByUserId(userId);

        for (Investments inv : investments) {

            BigDecimal initialValue = inv.getValue();

            BigDecimal monthlyRate = BigDecimal
                    .valueOf(inv.getExpectedReturn())
                    .divide(BigDecimal.valueOf(100), MathContext.DECIMAL64)
                    .divide(BigDecimal.valueOf(12), MathContext.DECIMAL64);

            LocalDate investStart = inv.getStartDate();
            LocalDate investEnd = inv.getEndDate();

            for (LocalDate date : projection.keySet()) {

                if (date.isBefore(investStart) || date.isAfter(investEnd)) {

                    continue;
                }

                long monthsPassed = ChronoUnit.MONTHS.between(YearMonth.from(investStart), YearMonth.from(date));

                BigDecimal growthFactor = BigDecimal.ONE.add(monthlyRate)
                        .pow((int) monthsPassed, MathContext.DECIMAL64);

                BigDecimal currentValue = initialValue.multiply(growthFactor, MathContext.DECIMAL64);

                MonthlyProjectionResponse item = projection.get(date);
                item.setInvestments(item.getInvestments().add(currentValue));
            }
        }

        return new ArrayList<>(projection.values());
    }

}
