package com.igorAugusto.domus.domus.service;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.igorAugusto.domus.domus.dto.InvestmentsRequest;
import com.igorAugusto.domus.domus.dto.InvestmentsResponse;
import com.igorAugusto.domus.domus.entity.Investments;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.InvestmentsRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class InvestmentsService {

    private final InvestmentsRepository investmentsRepository;
    private final UserRepository userRepository;

    public InvestmentsResponse createInvestment(InvestmentsRequest request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        Investments investments = Investments.builder()
                .value(request.getValue())
                .typeInvestments(request.getTypeInvestments())
                .createdAt(request.getCreatedAt())
                .user(user)
                .build();

        // 3. Salva no banco
        investments = investmentsRepository.save(investments);

        // 4. Retorna DTO de resposta
        return convertToResponse(investments);        
    }

    public List<InvestmentsResponse> getAllInvestments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return investmentsRepository.findByUserId(user.getId())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    public BigDecimal getTotalInvestments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        BigDecimal total = investmentsRepository.sumByUserId(user.getId());
        return total != null ? total : BigDecimal.ZERO;
    }

    private InvestmentsResponse convertToResponse(Investments investments) {
        return new InvestmentsResponse(
                investments.getId(),
                investments.getValue(),
                investments.getTypeInvestments(),
                investments.getCreatedAt());
    }
}
