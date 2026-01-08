package com.igorAugusto.domus.domus.service;

import com.igorAugusto.domus.domus.dto.IncomeRequest;
import com.igorAugusto.domus.domus.dto.IncomeResponse;
import com.igorAugusto.domus.domus.dto.OutgoingRequest;
import com.igorAugusto.domus.domus.dto.OutgoingResponse;
import com.igorAugusto.domus.domus.entity.Income;
import com.igorAugusto.domus.domus.entity.Outgoing;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutgoingService {

        private final OutgoingRepository outgoingRepository;
        private final UserRepository userRepository;

        // Criar despesa
        public OutgoingResponse createOutgoing(OutgoingRequest request, String userEmail) {
                // 1. Busca o usuário logado
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                Integer duration;

                if ("One-time".equals(request.getFrequency())) {
                        duration = 1;
                } else {
                        if (request.getDurationInMonths() == null || request.getDurationInMonths() <= 0) {
                                throw new IllegalArgumentException("Duração é obrigatória para despesas recorrentes");
                        }
                        duration = request.getDurationInMonths();
                }

                // 2. Cria a despesa
                Outgoing outgoing = Outgoing.builder()
                                .value(request.getValue())
                                .description(request.getDescription())
                                .startDate(request.getStartDate())
                                .durationInMonths(duration)
                                .category(request.getCategory())
                                .frequency(request.getFrequency())
                                .user(user)
                                .build();

                // 3. Salva no banco
                outgoing = outgoingRepository.save(outgoing);

                // 4. Retorna DTO de resposta
                return convertToResponse(outgoing);
        }

        // Listar todas as despesas do usuário
        public List<OutgoingResponse> getAllOutgoings(String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                return outgoingRepository.findByUserId(user.getId())
                                .stream()
                                .map(this::convertToResponse)
                                .toList();
        }

        // Calcular total de despesas
        public BigDecimal getTotalOutgoing(String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                BigDecimal total = outgoingRepository.sumByUserId(user.getId());
                return total != null ? total : BigDecimal.ZERO;
        }

        public OutgoingResponse updateOutgoing(
                        Long outgoingId,
                        OutgoingRequest request,
                        String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                Outgoing outgoing = outgoingRepository.findById(outgoingId)
                                .orElseThrow(() -> new RuntimeException("Despesa não encontrada"));

                if (!outgoing.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("Acesso negado");
                }

                outgoing.setValue(request.getValue());
                outgoing.setDescription(request.getDescription());
                outgoing.setStartDate(request.getStartDate());
                outgoing.setDurationInMonths(request.getDurationInMonths());
                outgoing.setFrequency(request.getFrequency());
                outgoing.setCategory(request.getCategory());

                Outgoing updated = outgoingRepository.save(outgoing);

                return convertToResponse(updated);
        }

        public void deleteOutgoing(Long outgoinId, String userEmail) {
                User user = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                Outgoing outgoing = outgoingRepository.findById(outgoinId)
                                .orElseThrow(() -> new RuntimeException("Despesa não encontrada"));

                if (!outgoing.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("Acesso negado");
                }

                outgoingRepository.delete(outgoing);
        }

        public void createFromImport(User user, Row row) {

                Outgoing outgoing = new Outgoing();
                outgoing.setUser(user);
                outgoing.setDescription(getString(row.getCell(1)));
                outgoing.setValue(getBigDecimal(row.getCell(2)));
                outgoing.setStartDate(LocalDate.parse(getString(row.getCell(3))));
                outgoing.setFrequency(getString(row.getCell(5)));
                outgoing.setDurationInMonths(getInt(row.getCell(6)));

                outgoingRepository.save(outgoing);
        }

        private String getString(Cell cell) {
                if (cell == null)
                        return null;
                if (cell.getCellType() == CellType.STRING) {
                        return cell.getStringCellValue().trim();
                }
                if (cell.getCellType() == CellType.NUMERIC) {
                        return String.valueOf((int) cell.getNumericCellValue());
                }
                return null;
        }

        private boolean getBoolean(Cell cell) {
                if (cell == null)
                        return false;
                if (cell.getCellType() == CellType.BOOLEAN) {
                        return cell.getBooleanCellValue();
                }
                if (cell.getCellType() == CellType.STRING) {
                        return Boolean.parseBoolean(cell.getStringCellValue());
                }
                return false;
        }

        private int getInt(Cell cell) {
                if (cell == null)
                        return 1;
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
                if (cell == null)
                        return BigDecimal.ZERO;
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

        // Converter Entity para DTO
        private OutgoingResponse convertToResponse(Outgoing outgoing) {
                return new OutgoingResponse(
                                outgoing.getId(),
                                outgoing.getValue(),
                                outgoing.getDescription(),
                                outgoing.getStartDate(),
                                outgoing.getDurationInMonths(),
                                outgoing.getCategory(),
                                outgoing.getCreatedAt(),
                                outgoing.getFrequency());
        }
}
