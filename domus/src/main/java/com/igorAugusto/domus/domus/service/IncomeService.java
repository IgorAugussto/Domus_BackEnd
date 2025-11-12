package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.entity.Income;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.IncomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class IncomeService {

    @Autowired
    private IncomeRepository incomeRepository;

    @Autowired
    private AuthService authService; // Assume que já existe para obter usuário do token

    public Income cadastrarReceita(IncomeDTO incomeDTO) {
        // Obtém o usuário logado do token JWT
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = authService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Cria a entidade Receita
        Income income = new Income();
        income.setUser(user);
        income.setValue(incomeDTO.valor());
        income.setDescriptions(incomeDTO.descricao());
        income.setDate(incomeDTO.data());

        // Salva no banco
        return incomeRepository.save(income);
    }
}
