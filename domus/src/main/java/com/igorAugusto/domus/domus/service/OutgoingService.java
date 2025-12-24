package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.OutgoingRequest;
import com.igorAugusto.domus.domus.dto.OutgoingResponse;
import com.igorAugusto.domus.domus.entity.Outgoing;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutgoingService {

    private final OutgoingRepository outgoingRepository;
    private final UserRepository userRepository;

    // Criar despesa
    public OutgoingResponse createOutgoing(OutgoingRequest request, String userEmail) {
        // 1. Busca o usuário logado
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Cria a despesa
        Outgoing outgoing = Outgoing.builder()
                .value(request.getValue())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .durationInMonths(request.getDurationInMonths())
                .category(request.getCategory())
                .frequency(request.getFrequency())
                .user(user)
                .build();

        if (!"One-time".equals(request.getFrequency())) {
            if (request.getDurationInMonths() == null || request.getDurationInMonths() <= 0) {
                throw new IllegalArgumentException("Duração é obrigatória para despesas recorrentes");
            }
        } else {
            // One-time → duração = 1 mês
            request.setDurationInMonths(1);
        }


        // 3. Salva no banco
        outgoing = outgoingRepository.save(outgoing);

        // 4. Retorna DTO de resposta
        return convertToResponse(outgoing);
    }

    // Listar todas as despesas do usuário
    public List<OutgoingResponse> getAllOutgoings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return outgoingRepository.findByUserId(user.getId())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Calcular total de despesas
    public BigDecimal getTotalOutgoing(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        BigDecimal total = outgoingRepository.sumByUserId(user.getId());
        return total != null ? total : BigDecimal.ZERO;
    }

    // Converter Entity para DTO
    private OutgoingResponse convertToResponse(Outgoing outgoing) {
        return new OutgoingResponse(
                outgoing.getId(),
                outgoing.getValue(),
                outgoing.getDescription(),
                outgoing.getStartDate(),
                outgoing.getDurationInMonths(),
                outgoing.getCategory(),
                outgoing.getCreatedAt(),
                outgoing.getFrequency()
        );
    }
}
