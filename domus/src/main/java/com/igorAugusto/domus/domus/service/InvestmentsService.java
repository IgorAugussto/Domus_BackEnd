package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.InvestmentsRequest;
import com.igorAugusto.domus.domus.dto.InvestmentsResponse;
import com.igorAugusto.domus.domus.entity.Investments;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.exception.ForbiddenException;
import com.igorAugusto.domus.domus.exception.ResourceNotFoundException;
import com.igorAugusto.domus.domus.repository.InvestmentsRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InvestmentsService {

        private final InvestmentsRepository investmentsRepository;
        private final UserRepository userRepository;

        @Transactional
        public InvestmentsResponse createInvestment(InvestmentsRequest request, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                Investments investments = Investments.builder()
                                .value(request.getValue())
                                .typeInvestments(request.getTypeInvestments())
                                .startDate(request.getStartDate())
                                .endDate(request.getEndDate())
                                .expectedReturn(request.getExpectedReturn())
                                .description(request.getDescription())
                                .user(user)
                                .build();

                return convertToResponse(investmentsRepository.save(investments));
        }

        @Transactional(readOnly = true)
        public Page<InvestmentsResponse> getAllInvestments(String userEmail, Pageable pageable) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                return investmentsRepository.findByUserId(user.getId(), pageable)
                                .map(this::convertToResponse);
        }

        @Transactional(readOnly = true)
        public BigDecimal getTotalInvestments(String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                BigDecimal total = investmentsRepository.sumByUserId(user.getId());
                return total != null ? total : BigDecimal.ZERO;
        }

        @Transactional
        public InvestmentsResponse updateInvestment(Long investmentsId, InvestmentsRequest request, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                Investments investments = investmentsRepository.findById(investmentsId)
                                .orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado"));

                if (!investments.getUser().getId().equals(user.getId())) {
                        throw new ForbiddenException("Acesso negado");
                }

                investments.setValue(request.getValue());
                investments.setDescription(request.getDescription());
                investments.setStartDate(request.getStartDate());
                investments.setEndDate(request.getEndDate());
                investments.setTypeInvestments(request.getTypeInvestments());
                investments.setExpectedReturn(request.getExpectedReturn());

                return convertToResponse(investmentsRepository.save(investments));
        }

        @Transactional
        public void deleteInvestment(Long investmentsId, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                Investments investments = investmentsRepository.findById(investmentsId)
                                .orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado"));

                if (!investments.getUser().getId().equals(user.getId())) {
                        throw new ForbiddenException("Acesso negado");
                }

                investmentsRepository.delete(investments);
        }

        private InvestmentsResponse convertToResponse(Investments investments) {
                return new InvestmentsResponse(
                                investments.getId(),
                                investments.getValue(),
                                investments.getTypeInvestments(),
                                investments.getCreatedAt(),
                                investments.getStartDate(),
                                investments.getEndDate(),
                                investments.getExpectedReturn(),
                                investments.getDescription());
        }
}
