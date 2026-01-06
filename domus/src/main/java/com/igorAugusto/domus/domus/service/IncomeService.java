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
import java.time.LocalDate;

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

                boolean isRecurring = false;
                LocalDate endDate = request.getEndDate();
                LocalDate startDate = request.getStartDate();

                if ("Monthly".equals(request.getFrequency())) {
                        isRecurring = true;

                        if (startDate == null) {
                                throw new IllegalArgumentException("Data de início é obrigatório para receitas recorrentes");
                        }

                        endDate = startDate.plusYears(1);
                }

                // 2. Cria a receita
                Income income = Income.builder()
                                .value(request.getValue())
                                .description(request.getDescription())
                                .startDate(startDate)
                                .endDate(endDate)
                                .recurring(isRecurring)
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

        public IncomeResponse updateIncome(Long incomeId, IncomeRequest request, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                Income income = incomeRepository.findById(incomeId)
                                .orElseThrow(() -> new RuntimeException("Income não encontrada"));

                if (!income.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("Acesso negado");
                }

                boolean isRecurring = false;
                LocalDate endDate = request.getEndDate();
                LocalDate startDate = request.getStartDate();

                if ("Monthly".equals(request.getFrequency())) {
                        isRecurring = true;

                        if (startDate == null) {
                                throw new IllegalArgumentException("Data de início é obrigatório para receitas recorrentes");
                        }

                        endDate = startDate.plusYears(1);
                }

                income.setValue(request.getValue());
                income.setDescription(request.getDescription());
                income.setStartDate(startDate);
                income.setEndDate(endDate);
                income.setFrequency(request.getFrequency());
                income.setCategory(request.getCategory());
                income.setRecurring(isRecurring);

                Income updated = incomeRepository.save(income);

                return convertToResponse(updated);
        }

        public void deleteIncome(Long incomeId, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                Income income = incomeRepository.findById(incomeId)
                                .orElseThrow(() -> new RuntimeException("Income não encontrada"));

                if (!income.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("Acesso negado");
                }

                incomeRepository.delete(income);
        }

        // Converter Entity para DTO
        private IncomeResponse convertToResponse(Income income) {
                return new IncomeResponse(
                                income.getId(),
                                income.getValue(),
                                income.getDescription(),
                                income.getStartDate(),
                                income.getEndDate(),
                                income.getRecurring(),
                                income.getCategory(),
                                income.getCreatedAt(),
                                income.getFrequency());
        }
}
