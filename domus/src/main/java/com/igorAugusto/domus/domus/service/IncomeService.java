package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.IncomeRequest;
import com.igorAugusto.domus.domus.dto.IncomeResponse;
import com.igorAugusto.domus.domus.entity.Income;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.enums.Frequency;
import com.igorAugusto.domus.domus.exception.BusinessException;
import com.igorAugusto.domus.domus.exception.ForbiddenException;
import com.igorAugusto.domus.domus.exception.ResourceNotFoundException;
import com.igorAugusto.domus.domus.repository.IncomeRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class IncomeService {

        private final IncomeRepository incomeRepository;
        private final UserRepository userRepository;

        @Transactional
        public IncomeResponse createIncome(IncomeRequest request, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                boolean isRecurring = false;
                LocalDate endDate = request.getEndDate();
                LocalDate startDate = request.getStartDate();

                if (Frequency.MONTHLY.equals(request.getFrequency())) {
                        isRecurring = true;

                        if (startDate == null) {
                                throw new BusinessException("Data de início é obrigatória para receitas recorrentes");
                        }

                        endDate = startDate.plusYears(1);
                }

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

                return convertToResponse(incomeRepository.save(income));
        }

        @Transactional(readOnly = true)
        public Page<IncomeResponse> getAllIncomes(String userEmail, Pageable pageable) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                return incomeRepository.findByUserId(user.getId(), pageable)
                                .map(this::convertToResponse);
        }

        @Transactional(readOnly = true)
        public BigDecimal getTotalIncome(String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                BigDecimal total = incomeRepository.sumByUserId(user.getId());
                return total != null ? total : BigDecimal.ZERO;
        }

        @Transactional
        public IncomeResponse updateIncome(Long incomeId, IncomeRequest request, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                Income income = incomeRepository.findById(incomeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Receita não encontrada"));

                if (!income.getUser().getId().equals(user.getId())) {
                        throw new ForbiddenException("Acesso negado");
                }

                boolean isRecurring = false;
                LocalDate endDate = request.getEndDate();
                LocalDate startDate = request.getStartDate();

                if (Frequency.MONTHLY.equals(request.getFrequency())) {
                        isRecurring = true;

                        if (startDate == null) {
                                throw new BusinessException("Data de início é obrigatória para receitas recorrentes");
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

                return convertToResponse(incomeRepository.save(income));
        }

        @Transactional
        public void deleteIncome(Long incomeId, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                Income income = incomeRepository.findById(incomeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Receita não encontrada"));

                if (!income.getUser().getId().equals(user.getId())) {
                        throw new ForbiddenException("Acesso negado");
                }

                incomeRepository.delete(income);
        }

        @Transactional
        public void createFromImport(User user, String[] cols) {
                Income income = new Income();
                income.setUser(user);
                income.setValue(new BigDecimal(cols[1]));
                income.setStartDate(LocalDate.parse(cols[2]));
                income.setRecurring(Boolean.parseBoolean(cols[3]));
                try {
                        income.setFrequency(Frequency.fromValue(cols[4]));
                } catch (IllegalArgumentException ignored) {
                        income.setFrequency(null);
                }

                incomeRepository.save(income);
        }

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
