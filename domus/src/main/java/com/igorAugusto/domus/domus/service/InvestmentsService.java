package com.igorAugusto.domus.domus.service;


import com.igorAugusto.domus.domus.dto.InvestmentsRequest;
import com.igorAugusto.domus.domus.dto.InvestmentsResponse;
import com.igorAugusto.domus.domus.entity.Investments;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.InvestmentsRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

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
                                .startDate(request.getStartDate())
                                .endDate(request.getEndDate())
                                .expectedReturn(request.getExpectedReturn())
                                .description(request.getDescription())
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

        public InvestmentsResponse updateInvestment(Long investmentsId, InvestmentsRequest request, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                Investments investments = investmentsRepository.findById(investmentsId)
                                .orElseThrow(() -> new RuntimeException("Investimentos não encontrados"));

                if (!investments.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("Acesso negado");
                }

                investments.setValue(request.getValue());
                investments.setDescription(request.getDescription());
                investments.setStartDate(request.getStartDate());
                investments.setEndDate(request.getEndDate());
                investments.setTypeInvestments(request.getTypeInvestments());

                Investments updated = investmentsRepository.save(investments);

                return convertToResponse(updated);
        }

        public void deleteInvestment(Long investmentsId, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                Investments investments = investmentsRepository.findById(investmentsId)
                                .orElseThrow(() -> new RuntimeException("Investimentos não encontrados"));

                if (!investments.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("Acesso negado");
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
