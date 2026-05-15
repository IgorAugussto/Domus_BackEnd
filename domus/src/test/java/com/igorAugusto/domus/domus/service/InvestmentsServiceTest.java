package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.InvestmentsRequest;
import com.igorAugusto.domus.domus.dto.InvestmentsResponse;
import com.igorAugusto.domus.domus.entity.Investments;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.exception.ForbiddenException;
import com.igorAugusto.domus.domus.exception.ResourceNotFoundException;
import com.igorAugusto.domus.domus.repository.InvestmentsRepository;
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
class InvestmentsServiceTest {

    @Mock
    private InvestmentsRepository investmentsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InvestmentsService investmentsService;

    private User user;
    private Investments investment;
    private InvestmentsRequest request;

    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2027, 1, 1);
    private static final String USER_EMAIL = "test@domus.app";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email(USER_EMAIL)
                .name("Test User")
                .password("encoded")
                .build();

        investment = Investments.builder()
                .id(1L)
                .value(BigDecimal.valueOf(1000.00))
                .typeInvestments("ITAUSA4")
                .startDate(START_DATE)
                .endDate(END_DATE)
                .expectedReturn(12.5)
                .description("Ação Itaúsa")
                .user(user)
                .build();

        request = new InvestmentsRequest(
                BigDecimal.valueOf(1000.00), "ITAUSA4", START_DATE, END_DATE, "Ação Itaúsa", 12.5
        );
    }

    // ─── createInvestment ────────────────────────────────────────────────────

    @Test
    void createInvestment_success() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(investmentsRepository.save(any(Investments.class))).thenReturn(investment);

        InvestmentsResponse response = investmentsService.createInvestment(request, USER_EMAIL);

        assertThat(response).isNotNull();
        assertThat(response.getTypeInvestments()).isEqualTo("ITAUSA4");
        assertThat(response.getValue()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
        assertThat(response.getExpectedReturn()).isEqualTo(12.5);
        verify(investmentsRepository, times(1)).save(any(Investments.class));
    }

    @Test
    void createInvestment_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("unknown@domus.app")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentsService.createInvestment(request, "unknown@domus.app"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");

        verify(investmentsRepository, never()).save(any());
    }

    // ─── getAllInvestments ────────────────────────────────────────────────────

    @Test
    void getAllInvestments_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Investments> page = new PageImpl<>(List.of(investment), pageable, 1);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(investmentsRepository.findByUserId(1L, pageable)).thenReturn(page);

        Page<InvestmentsResponse> result = investmentsService.getAllInvestments(USER_EMAIL, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTypeInvestments()).isEqualTo("ITAUSA4");
    }

    @Test
    void getAllInvestments_userNotFound_throwsResourceNotFoundException() {
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findByEmail("unknown@domus.app")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentsService.getAllInvestments("unknown@domus.app", pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    // ─── getTotalInvestments ──────────────────────────────────────────────────

    @Test
    void getTotalInvestments_returnsCorrectSum() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(investmentsRepository.sumByUserId(1L)).thenReturn(BigDecimal.valueOf(5000.00));

        BigDecimal total = investmentsService.getTotalInvestments(USER_EMAIL);

        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(5000.00));
    }

    @Test
    void getTotalInvestments_repositoryReturnsNull_returnsZero() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(investmentsRepository.sumByUserId(1L)).thenReturn(null);

        BigDecimal total = investmentsService.getTotalInvestments(USER_EMAIL);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─── updateInvestment ─────────────────────────────────────────────────────

    @Test
    void updateInvestment_success() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(investmentsRepository.findById(1L)).thenReturn(Optional.of(investment));
        when(investmentsRepository.save(any(Investments.class))).thenReturn(investment);

        InvestmentsResponse response = investmentsService.updateInvestment(1L, request, USER_EMAIL);

        assertThat(response).isNotNull();
        verify(investmentsRepository, times(1)).save(any(Investments.class));
    }

    @Test
    void updateInvestment_investmentNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(investmentsRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentsService.updateInvestment(99L, request, USER_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Investimento não encontrado");

        verify(investmentsRepository, never()).save(any());
    }

    @Test
    void updateInvestment_investmentBelongsToOtherUser_throwsForbiddenException() {
        User otherUser = User.builder().id(2L).email("other@domus.app").build();
        Investments otherInvestment = Investments.builder().id(1L).user(otherUser).build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(investmentsRepository.findById(1L)).thenReturn(Optional.of(otherInvestment));

        assertThatThrownBy(() -> investmentsService.updateInvestment(1L, request, USER_EMAIL))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Acesso negado");

        verify(investmentsRepository, never()).save(any());
    }

    // ─── deleteInvestment ─────────────────────────────────────────────────────

    @Test
    void deleteInvestment_success() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(investmentsRepository.findById(1L)).thenReturn(Optional.of(investment));

        investmentsService.deleteInvestment(1L, USER_EMAIL);

        verify(investmentsRepository, times(1)).delete(investment);
    }

    @Test
    void deleteInvestment_investmentNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(investmentsRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentsService.deleteInvestment(99L, USER_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Investimento não encontrado");

        verify(investmentsRepository, never()).delete(any());
    }

    @Test
    void deleteInvestment_investmentBelongsToOtherUser_throwsForbiddenException() {
        User otherUser = User.builder().id(2L).email("other@domus.app").build();
        Investments otherInvestment = Investments.builder().id(1L).user(otherUser).build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(investmentsRepository.findById(1L)).thenReturn(Optional.of(otherInvestment));

        assertThatThrownBy(() -> investmentsService.deleteInvestment(1L, USER_EMAIL))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Acesso negado");

        verify(investmentsRepository, never()).delete(any());
    }
}
