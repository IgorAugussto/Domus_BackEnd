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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IncomeService incomeService;

    private User user;
    private Income income;
    private final LocalDate startDate = LocalDate.of(2026, 1, 1);

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@domus.app")
                .name("Test User")
                .password("encoded")
                .build();

        income = Income.builder()
                .id(1L)
                .value(BigDecimal.valueOf(5000.00))
                .description("Salário")
                .startDate(startDate)
                .endDate(startDate.plusYears(1))
                .recurring(true)
                .frequency(Frequency.MONTHLY)
                .category("Salário")
                .user(user)
                .build();
    }

    @Test
    void createIncome_monthly_success() {
        IncomeRequest request = new IncomeRequest(
                BigDecimal.valueOf(5000.00), "Salário", startDate,
                null, null, Frequency.MONTHLY, "Salário"
        );

        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(incomeRepository.save(any(Income.class))).thenReturn(income);

        IncomeResponse response = incomeService.createIncome(request, "test@domus.app");

        assertThat(response).isNotNull();
        assertThat(response.getDescription()).isEqualTo("Salário");
        assertThat(response.getFrequency()).isEqualTo(Frequency.MONTHLY);
        verify(incomeRepository, times(1)).save(any(Income.class));
    }

    @Test
    void createIncome_monthly_withoutStartDate_throwsBusinessException() {
        IncomeRequest request = new IncomeRequest(
                BigDecimal.valueOf(5000.00), "Salário", null,
                null, null, Frequency.MONTHLY, "Salário"
        );

        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> incomeService.createIncome(request, "test@domus.app"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Data de início é obrigatória para receitas recorrentes");
    }

    @Test
    void createIncome_userNotFound_throwsResourceNotFoundException() {
        IncomeRequest request = new IncomeRequest(
                BigDecimal.valueOf(5000.00), "Salário", startDate,
                null, null, Frequency.MONTHLY, "Salário"
        );

        when(userRepository.findByEmail("unknown@domus.app")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeService.createIncome(request, "unknown@domus.app"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");

        verify(incomeRepository, never()).save(any());
    }

    @Test
    void getAllIncomes_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Income> page = new PageImpl<>(List.of(income), pageable, 1);

        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(incomeRepository.findByUserId(1L, pageable)).thenReturn(page);

        Page<IncomeResponse> result = incomeService.getAllIncomes("test@domus.app", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Salário");
    }

    @Test
    void updateIncome_accessDenied_throwsForbiddenException() {
        User otherUser = User.builder().id(2L).email("other@domus.app").build();
        Income otherIncome = Income.builder().id(1L).user(otherUser).build();

        IncomeRequest request = new IncomeRequest(
                BigDecimal.valueOf(6000.00), "Salário", startDate,
                null, null, Frequency.MONTHLY, "Salário"
        );

        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(incomeRepository.findById(1L)).thenReturn(Optional.of(otherIncome));

        assertThatThrownBy(() -> incomeService.updateIncome(1L, request, "test@domus.app"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Acesso negado");

        verify(incomeRepository, never()).save(any());
    }

    @Test
    void deleteIncome_notFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(incomeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeService.deleteIncome(99L, "test@domus.app"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Receita não encontrada");

        verify(incomeRepository, never()).delete(any());
    }

    @Test
    void deleteIncome_success() {
        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(incomeRepository.findById(1L)).thenReturn(Optional.of(income));

        incomeService.deleteIncome(1L, "test@domus.app");

        verify(incomeRepository, times(1)).delete(income);
    }
}
