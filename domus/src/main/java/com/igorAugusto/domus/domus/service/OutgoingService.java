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
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OutgoingService {

        private final OutgoingRepository outgoingRepository;
        private final UserRepository userRepository;

        @Transactional
        public OutgoingResponse createOutgoing(OutgoingRequest request, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                Integer duration;

                if (Frequency.ONE_TIME.equals(request.getFrequency())) {
                        duration = 1;
                } else {
                        if (request.getDurationInMonths() == null || request.getDurationInMonths() <= 0) {
                                throw new BusinessException("Duração é obrigatória para despesas recorrentes");
                        }
                        duration = request.getDurationInMonths();
                }

                Outgoing outgoing = Outgoing.builder()
                                .value(request.getValue())
                                .description(request.getDescription())
                                .startDate(request.getStartDate())
                                .durationInMonths(duration)
                                .category(request.getCategory())
                                .frequency(request.getFrequency())
                                .paymentType(request.getPaymentType())
                                .paid(request.getPaid() != null ? request.getPaid() : false)
                                .user(user)
                                .build();

                return convertToResponse(outgoingRepository.save(outgoing));
        }

        @Transactional(readOnly = true)
        public Page<OutgoingResponse> getAllOutgoings(String userEmail, Pageable pageable) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                return outgoingRepository.findByUserId(user.getId(), pageable)
                                .map(this::convertToResponse);
        }

        @Transactional(readOnly = true)
        public BigDecimal getTotalOutgoing(String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                BigDecimal total = outgoingRepository.sumByUserId(user.getId());
                return total != null ? total : BigDecimal.ZERO;
        }

        @Transactional
        public OutgoingResponse updateOutgoing(Long outgoingId, OutgoingRequest request, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                Outgoing outgoing = outgoingRepository.findById(outgoingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada"));

                if (!outgoing.getUser().getId().equals(user.getId())) {
                        throw new ForbiddenException("Acesso negado");
                }

                outgoing.setValue(request.getValue());
                outgoing.setDescription(request.getDescription());
                outgoing.setStartDate(request.getStartDate());
                outgoing.setDurationInMonths(request.getDurationInMonths());
                outgoing.setFrequency(request.getFrequency());
                outgoing.setCategory(request.getCategory());
                outgoing.setPaymentType(request.getPaymentType());
                outgoing.setPaid(request.getPaid() != null ? request.getPaid() : false);

                return convertToResponse(outgoingRepository.save(outgoing));
        }

        @Transactional
        public void deleteOutgoing(Long outgoingId, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

                Outgoing outgoing = outgoingRepository.findById(outgoingId)
                                .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada"));

                if (!outgoing.getUser().getId().equals(user.getId())) {
                        throw new ForbiddenException("Acesso negado");
                }

                outgoingRepository.delete(outgoing);
        }

        @Transactional
        public void createFromImport(User user, Row row) {
                Outgoing outgoing = new Outgoing();
                outgoing.setUser(user);
                outgoing.setDescription(getString(row.getCell(1)));
                outgoing.setValue(getBigDecimal(row.getCell(2)));
                outgoing.setStartDate(LocalDate.parse(getString(row.getCell(3))));
                try {
                        outgoing.setFrequency(Frequency.fromValue(getString(row.getCell(5))));
                } catch (IllegalArgumentException ignored) {
                        outgoing.setFrequency(null);
                }
                outgoing.setDurationInMonths(getInt(row.getCell(6)));

                outgoingRepository.save(outgoing);
        }

        private String getString(Cell cell) {
                if (cell == null) return null;
                if (cell.getCellType() == CellType.STRING) {
                        return cell.getStringCellValue().trim();
                }
                if (cell.getCellType() == CellType.NUMERIC) {
                        return String.valueOf((int) cell.getNumericCellValue());
                }
                return null;
        }

        private int getInt(Cell cell) {
                if (cell == null) return 1;
                if (cell.getCellType() == CellType.NUMERIC) {
                        return (int) cell.getNumericCellValue();
                }
                if (cell.getCellType() == CellType.STRING) {
                        try {
                                return Integer.parseInt(cell.getStringCellValue());
                        } catch (NumberFormatException ignored) {
                        }
                }
                return 1;
        }

        private BigDecimal getBigDecimal(Cell cell) {
                if (cell == null) return BigDecimal.ZERO;
                if (cell.getCellType() == CellType.NUMERIC) {
                        return BigDecimal.valueOf(cell.getNumericCellValue());
                }
                if (cell.getCellType() == CellType.STRING) {
                        try {
                                return new BigDecimal(cell.getStringCellValue());
                        } catch (NumberFormatException ignored) {
                        }
                }
                return BigDecimal.ZERO;
        }

        private OutgoingResponse convertToResponse(Outgoing outgoing) {
                return new OutgoingResponse(
                                outgoing.getId(),
                                outgoing.getValue(),
                                outgoing.getDescription(),
                                outgoing.getStartDate(),
                                outgoing.getDurationInMonths(),
                                outgoing.getCategory(),
                                outgoing.getCreatedAt(),
                                outgoing.getFrequency(),
                                outgoing.getPaymentType(),
                                outgoing.getPaid()
                );
        }
}
