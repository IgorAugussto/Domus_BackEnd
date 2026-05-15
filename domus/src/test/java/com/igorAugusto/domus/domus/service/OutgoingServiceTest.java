package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.OutgoingRequest;
import com.igorAugusto.domus.domus.dto.OutgoingResponse;
import com.igorAugusto.domus.domus.entity.Outgoing;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.enums.Frequency;
import com.igorAugusto.domus.domus.exception.BusinessException;
import com.igorAugusto.domus.domus.exception.ForbiddenException;
import com.igorAugusto.domus.domus.exception.ResourceNotFoundException;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutgoingServiceTest {

    @Mock
    private OutgoingRepository outgoingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OutgoingService outgoingService;

    private User user;
    private Outgoing outgoing;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@domus.app")
                .name("Test User")
                .password("encoded")
                .build();

        outgoing = Outgoing.builder()
                .id(1L)
                .value(BigDecimal.valueOf(100.00))
                .description("Aluguel")
                .startDate(LocalDate.now())
                .frequency(Frequency.MONTHLY)
                .durationInMonths(12)
                .category("Moradia")
                .paymentType("Boleto")
                .paid(false)
                .user(user)
                .build();
    }

    @Test
    void createOutgoing_success() {
        OutgoingRequest request = new OutgoingRequest(
                BigDecimal.valueOf(100.00), "Aluguel", LocalDate.now(),
                12, Frequency.MONTHLY, "Moradia", "Boleto", false
        );

        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(outgoingRepository.save(any(Outgoing.class))).thenReturn(outgoing);

        OutgoingResponse response = outgoingService.createOutgoing(request, "test@domus.app");

        assertThat(response).isNotNull();
        assertThat(response.getDescription()).isEqualTo("Aluguel");
        assertThat(response.getFrequency()).isEqualTo(Frequency.MONTHLY);
        verify(outgoingRepository, times(1)).save(any(Outgoing.class));
    }

    @Test
    void createOutgoing_userNotFound_throwsResourceNotFoundException() {
        OutgoingRequest request = new OutgoingRequest(
                BigDecimal.valueOf(100.00), "Aluguel", LocalDate.now(),
                12, Frequency.MONTHLY, "Moradia", "Boleto", false
        );

        when(userRepository.findByEmail("unknown@domus.app")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> outgoingService.createOutgoing(request, "unknown@domus.app"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");

        verify(outgoingRepository, never()).save(any());
    }

    @Test
    void createOutgoing_monthlyWithoutDuration_throwsBusinessException() {
        OutgoingRequest request = new OutgoingRequest(
                BigDecimal.valueOf(100.00), "Aluguel", LocalDate.now(),
                null, Frequency.MONTHLY, "Moradia", "Boleto", false
        );

        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> outgoingService.createOutgoing(request, "test@domus.app"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Duração é obrigatória para despesas recorrentes");
    }

    @Test
    void createOutgoing_oneTime_setsDurationToOne() {
        OutgoingRequest request = new OutgoingRequest(
                BigDecimal.valueOf(50.00), "Compra única", LocalDate.now(),
                null, Frequency.ONE_TIME, "Lazer", null, true
        );

        Outgoing oneTimeOutgoing = Outgoing.builder()
                .id(2L).value(BigDecimal.valueOf(50.00)).description("Compra única")
                .startDate(LocalDate.now()).frequency(Frequency.ONE_TIME)
                .durationInMonths(1).category("Lazer").paid(true).user(user).build();

        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(outgoingRepository.save(any(Outgoing.class))).thenReturn(oneTimeOutgoing);

        OutgoingResponse response = outgoingService.createOutgoing(request, "test@domus.app");

        assertThat(response.getDurationInMonths()).isEqualTo(1);
    }

    @Test
    void getAllOutgoings_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Outgoing> page = new PageImpl<>(List.of(outgoing), pageable, 1);

        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(outgoingRepository.findByUserId(1L, pageable)).thenReturn(page);

        Page<OutgoingResponse> result = outgoingService.getAllOutgoings("test@domus.app", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Aluguel");
    }

    @Test
    void updateOutgoing_accessDenied_throwsForbiddenException() {
        User otherUser = User.builder().id(2L).email("other@domus.app").build();
        Outgoing otherOutgoing = Outgoing.builder().id(1L).user(otherUser).build();

        OutgoingRequest request = new OutgoingRequest(
                BigDecimal.valueOf(200.00), "Aluguel", LocalDate.now(),
                12, Frequency.MONTHLY, "Moradia", "Boleto", false
        );

        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(outgoingRepository.findById(1L)).thenReturn(Optional.of(otherOutgoing));

        assertThatThrownBy(() -> outgoingService.updateOutgoing(1L, request, "test@domus.app"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Acesso negado");

        verify(outgoingRepository, never()).save(any());
    }

    @Test
    void deleteOutgoing_notFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(outgoingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> outgoingService.deleteOutgoing(99L, "test@domus.app"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Despesa não encontrada");

        verify(outgoingRepository, never()).delete(any());
    }

    @Test
    void deleteOutgoing_success() {
        when(userRepository.findByEmail("test@domus.app")).thenReturn(Optional.of(user));
        when(outgoingRepository.findById(1L)).thenReturn(Optional.of(outgoing));

        outgoingService.deleteOutgoing(1L, "test@domus.app");

        verify(outgoingRepository, times(1)).delete(outgoing);
    }
}
