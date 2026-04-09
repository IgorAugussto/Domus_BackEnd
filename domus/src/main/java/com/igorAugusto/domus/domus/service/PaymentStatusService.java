package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.PaymentMonthResponse;
import com.igorAugusto.domus.domus.entity.Outgoing;
import com.igorAugusto.domus.domus.entity.PaymentStatus;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
import com.igorAugusto.domus.domus.repository.PaymentStatusRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final PaymentStatusRepository paymentStatusRepository;
    private final OutgoingRepository outgoingRepository;
    private final UserRepository userRepository;

    /**
     * Retorna todas as despesas ativas em um mês específico com seus status.
     * Uma despesa é "ativa" em um mês se:
     * - É One-time: o mês é igual ao mês do startDate
     * - É Monthly: o mês está entre startDate e startDate + durationInMonths - 1
     */
    public List<PaymentMonthResponse> getPaymentsByMonth(String userEmail, String yearMonth) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        YearMonth targetMonth = YearMonth.parse(yearMonth);

        // Busca todas as despesas do usuário que são Cartão de Crédito ou Boleto
        List<Outgoing> outgoings = outgoingRepository.findByUserId(user.getId())
                .stream()
                .filter(o -> o.getPaymentType() != null &&
                        (o.getPaymentType().equals("Cartão de Crédito") ||
                                o.getPaymentType().equals("Boleto")))
                .toList();

        return outgoings.stream()
                .filter(outgoing -> isActiveInMonth(outgoing, targetMonth))
                .map(outgoing -> {
                    // Busca o status específico desse mês — se não existir, é false (pendente)
                    Optional<PaymentStatus> status = paymentStatusRepository
                            .findByOutgoingIdAndYearMonth(outgoing.getId(), yearMonth);

                    boolean paid = status.map(PaymentStatus::getPaid).orElse(false);

                    return new PaymentMonthResponse(
                            outgoing.getId(),
                            outgoing.getDescription(),
                            outgoing.getValue(),
                            outgoing.getCategory(),
                            outgoing.getPaymentType(),
                            outgoing.getFrequency(),
                            outgoing.getStartDate().toString(),
                            yearMonth,
                            paid
                    );
                })
                .toList();
    }

    /**
     * Marca uma despesa como paga ou pendente em um mês específico.
     * Usa upsert: cria o registro se não existir, atualiza se já existir.
     */
    public PaymentMonthResponse togglePaymentStatus(
            String userEmail,
            Long outgoingId,
            String yearMonth,
            boolean paid) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Outgoing outgoing = outgoingRepository.findById(outgoingId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // Garante que a despesa pertence ao usuário
        if (!outgoing.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Upsert: atualiza se existir, cria se não existir
        PaymentStatus status = paymentStatusRepository
                .findByOutgoingIdAndYearMonth(outgoingId, yearMonth)
                .orElse(PaymentStatus.builder()
                        .outgoing(outgoing)
                        .yearMonth(yearMonth)
                        .build());

        status.setPaid(paid);
        paymentStatusRepository.save(status);

        return new PaymentMonthResponse(
                outgoing.getId(),
                outgoing.getDescription(),
                outgoing.getValue(),
                outgoing.getCategory(),
                outgoing.getPaymentType(),
                outgoing.getFrequency(),
                outgoing.getStartDate().toString(),
                yearMonth,
                paid
        );
    }

    /**
     * Verifica se uma despesa está ativa em um mês específico.
     */
    private boolean isActiveInMonth(Outgoing outgoing, YearMonth targetMonth) {
        YearMonth startMonth = YearMonth.from(outgoing.getStartDate());

        if ("One-time".equals(outgoing.getFrequency())) {
            return startMonth.equals(targetMonth);
        }

        // Monthly: ativa entre startMonth e startMonth + durationInMonths - 1
        YearMonth endMonth = startMonth.plusMonths(outgoing.getDurationInMonths() - 1);
        return !targetMonth.isBefore(startMonth) && !targetMonth.isAfter(endMonth);
    }
}