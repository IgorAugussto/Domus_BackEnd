package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.IncomeRequest;
import com.igorAugusto.domus.domus.dto.IncomeResponse;
import com.igorAugusto.domus.domus.entity.Income;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.IncomeRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;

    // Criar receita
    public IncomeResponse createIncome(IncomeRequest request, String userEmail) {
        // 1. Busca o usuário logado
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Cria a receita
        Income income = Income.builder()
                .value(request.getValue())
                .description(request.getDescription())
                .date(request.getDate())
                .category(request.getCategory())
                .frequency(request.getFrequency())
                .user(user)
                .build();

        // 3. Salva no banco
        income = incomeRepository.save(income);

        // 4. Retorna DTO de resposta
        return convertToResponse(income);
    }

    // Listar todas as receitas do usuário
    public List<IncomeResponse> getAllIncomes(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        return incomeRepository.findByUserId(user.getId())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Calcular total de receitas
    public BigDecimal getTotalIncome(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        BigDecimal total = incomeRepository.sumByUserId(user.getId());
        return total != null ? total : BigDecimal.ZERO;
    }

    // Converter Entity para DTO
    private IncomeResponse convertToResponse(Income income) {
        return new IncomeResponse(
                income.getId(),
                income.getValue(),
                income.getDescription(),
                income.getDate(),
                income.getCategory(),
                income.getCreatedAt(),
                income.getFrequency()
        );
    }
}
