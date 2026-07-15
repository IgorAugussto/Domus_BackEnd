package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.PaymentMonthResponse;
import com.igorAugusto.domus.domus.entity.Outgoing;
import com.igorAugusto.domus.domus.entity.PaymentStatus;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.enums.Frequency;
import com.igorAugusto.domus.domus.exception.ForbiddenException;
import com.igorAugusto.domus.domus.exception.ResourceNotFoundException;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
import com.igorAugusto.domus.domus.repository.PaymentStatusRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final PaymentStatusRepository paymentStatusRepository;
    private final OutgoingRepository outgoingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PaymentMonthResponse> getPaymentsByMonth(String userEmail, String yearMonth) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        YearMonth targetMonth = YearMonth.parse(yearMonth);

        List<Outgoing> outgoings = outgoingRepository.findByUserId(user.getId(), Pageable.unpaged())
                .stream()
                .filter(o -> o.getPaymentType() != null &&
                        (o.getPaymentType().equals("Cartão de Crédito") ||
                                o.getPaymentType().equals("Boleto")))
                .toList();

        return outgoings.stream()
                .filter(outgoing -> isActiveInMonth(outgoing, targetMonth))
                .map(outgoing -> {
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
                            paid,
                            outgoing.getCreditCard() != null ? outgoing.getCreditCard().getId() : null
                    );
                })
                .toList();
    }

    @Transactional
    public PaymentMonthResponse togglePaymentStatus(
            String userEmail,
            Long outgoingId,
            String yearMonth,
            boolean paid) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Outgoing outgoing = outgoingRepository.findById(outgoingId)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada"));

        if (!outgoing.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Acesso negado");
        }

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
                paid,
                outgoing.getCreditCard() != null ? outgoing.getCreditCard().getId() : null
        );
    }

    private boolean isActiveInMonth(Outgoing outgoing, YearMonth targetMonth) {
        YearMonth startMonth = YearMonth.from(outgoing.getStartDate());

        if (Frequency.ONE_TIME.equals(outgoing.getFrequency())) {
            return startMonth.equals(targetMonth);
        }

        YearMonth endMonth = startMonth.plusMonths(outgoing.getDurationInMonths() - 1);
        return !targetMonth.isBefore(startMonth) && !targetMonth.isAfter(endMonth);
    }
}
