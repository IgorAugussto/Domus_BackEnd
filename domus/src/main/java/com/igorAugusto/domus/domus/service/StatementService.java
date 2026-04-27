package com.igorAugusto.domus.domus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igorAugusto.domus.domus.dto.StatementImportResponse;
import com.igorAugusto.domus.domus.entity.Outgoing;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final OutgoingRepository outgoingRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${statement.service.url:http://localhost:5000}")
    private String statementServiceUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatementImportResponse importStatement(
            MultipartFile file,
            String dueDate,   // ✅ data de vencimento definida pelo usuário
            String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate startDate    = LocalDate.parse(dueDate);
        JsonNode pythonResponse = callPythonService(file);
        JsonNode transactions  = pythonResponse.get("transacoes");
        int total              = pythonResponse.get("total").asInt();

        List<String> errors = new ArrayList<>();
        int saved   = 0;
        int skipped = 0;

        // Carrega uma vez todos os lançamentos mensais do usuário para verificar
        // se uma parcela importada já está coberta por uma projeção existente.
        List<Outgoing> existingMonthlyOutgoings = outgoingRepository
                .findByUserIdAndFrequency(user.getId(), "Monthly");
        YearMonth importMonth = YearMonth.from(startDate);

        for (JsonNode transaction : transactions) {
            try {
                String description   = transaction.get("description").asText();
                double amount        = transaction.get("amount").asDouble();
                String category      = transaction.get("category").asText();
                String frequency     = transaction.get("frequency").asText();
                String paymentType   = transaction.get("paymentType").asText();
                boolean paid         = transaction.get("paid").asBoolean();
                int durationInMonths = transaction.get("durationInMonths").asInt(1);

                // Data original da transação no arquivo (para deduplicação)
                String transactionDateStr = transaction.has("startDate")
                        ? transaction.get("startDate").asText() : "";
                LocalDate transactionDate = null;
                try {
                    if (!transactionDateStr.isBlank()) {
                        transactionDate = LocalDate.parse(transactionDateStr);
                    }
                } catch (Exception ignored) {}

                // Chave: userId + dataOriginal + descrição normalizada + valor
                String hashInput = user.getId() + "|" + transactionDateStr + "|"
                        + description.toLowerCase().trim() + "|"
                        + String.format("%.2f", amount);
                String importHash = DigestUtils.md5DigestAsHex(
                        hashInput.getBytes(StandardCharsets.UTF_8));

                if (outgoingRepository.existsByImportHash(importHash)) {
                    skipped++;
                    continue;
                }

                // Para parcelas mensais: verifica se o mês desta importação já está
                // coberto por um lançamento existente com mesma descrição e valor.
                // Isso evita duplicar projeções ao importar extratos de meses consecutivos.
                if ("Monthly".equals(frequency)) {
                    boolean alreadyCovered = existingMonthlyOutgoings.stream().anyMatch(o -> {
                        if (!o.getDescription().trim().equalsIgnoreCase(description.trim())) return false;
                        if (o.getValue().compareTo(BigDecimal.valueOf(amount)) != 0) return false;
                        YearMonth projStart = YearMonth.from(o.getStartDate());
                        YearMonth projEnd   = projStart.plusMonths(o.getDurationInMonths() - 1);
                        return !importMonth.isBefore(projStart) && !importMonth.isAfter(projEnd);
                    });
                    if (alreadyCovered) {
                        skipped++;
                        continue;
                    }
                }

                Outgoing outgoing = Outgoing.builder()
                        .value(BigDecimal.valueOf(amount))
                        .description(description)
                        .startDate(startDate)
                        .category(category)
                        .frequency(frequency)
                        .durationInMonths(durationInMonths)
                        .paymentType(paymentType)
                        .paid(paid)
                        .importHash(importHash)
                        .transactionDate(transactionDate)
                        .user(user)
                        .build();

                Outgoing saved_outgoing = outgoingRepository.save(outgoing);
                if ("Monthly".equals(frequency)) {
                    existingMonthlyOutgoings.add(saved_outgoing);
                }
                saved++;

            } catch (Exception e) {
                errors.add("Error saving transaction: " + e.getMessage());
            }
        }

        return new StatementImportResponse(total, saved, skipped, errors);
    }

    private JsonNode callPythonService(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    statementServiceUrl + "/processar-extrato",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            throw new RuntimeException("Error communicating with Python service: " + e.getMessage());
        }
    }
}