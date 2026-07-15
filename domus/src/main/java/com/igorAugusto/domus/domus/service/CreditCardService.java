package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.CreditCardRequest;
import com.igorAugusto.domus.domus.dto.CreditCardResponse;
import com.igorAugusto.domus.domus.entity.CreditCard;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.exception.ForbiddenException;
import com.igorAugusto.domus.domus.exception.ResourceNotFoundException;
import com.igorAugusto.domus.domus.repository.CreditCardRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreditCardResponse createCreditCard(CreditCardRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        CreditCard creditCard = CreditCard.builder()
                .brand(request.getBrand())
                .lastFourDigits(request.getLastFourDigits())
                .nickname(request.getNickname())
                .user(user)
                .build();

        return convertToResponse(creditCardRepository.save(creditCard));
    }

    @Transactional(readOnly = true)
    public List<CreditCardResponse> getActiveCards(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return creditCardRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Transactional
    public void deactivateCreditCard(Long creditCardId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        CreditCard creditCard = creditCardRepository.findById(creditCardId)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado"));

        if (!creditCard.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Acesso negado");
        }

        creditCard.setActive(false);
        creditCardRepository.save(creditCard);
    }

    private CreditCardResponse convertToResponse(CreditCard creditCard) {
        return new CreditCardResponse(
                creditCard.getId(),
                creditCard.getBrand(),
                creditCard.getLastFourDigits(),
                creditCard.getNickname(),
                creditCard.getActive()
        );
    }
}
