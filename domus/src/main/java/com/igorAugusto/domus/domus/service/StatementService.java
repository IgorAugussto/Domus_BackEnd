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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    public StatementImportResponse importStatement(MultipartFile file, String userEmail) {

        // 1. Busca o usuário logado
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Envia o arquivo para o microserviço Python processar
        JsonNode pythonResponse = callPythonService(file);

        // 3. Processa as transações retornadas pelo Python
        JsonNode transactions = pythonResponse.get("transacoes");
        int total = pythonResponse.get("total").asInt();

        List<String> errors = new ArrayList<>();
        int saved = 0;

        for (JsonNode transaction : transactions) {
            try {
                String description  = transaction.get("description").asText();
                double amount       = transaction.get("amount").asDouble();
                String startDateStr = transaction.get("startDate").asText();
                String category     = transaction.get("category").asText();
                String frequency    = transaction.get("frequency").asText();
                String paymentType  = transaction.get("paymentType").asText();
                boolean paid        = transaction.get("paid").asBoolean();

                Outgoing outgoing = Outgoing.builder()
                        .value(BigDecimal.valueOf(amount))
                        .description(description)
                        .startDate(LocalDate.parse(startDateStr))
                        .category(category)
                        .frequency(frequency)
                        .durationInMonths(1)      // extrato é sempre One-time
                        .paymentType(paymentType)
                        .paid(paid)
                        .user(user)
                        .build();

                outgoingRepository.save(outgoing);
                saved++;

            } catch (Exception e) {
                errors.add("Error saving transaction: " + e.getMessage());
            }
        }

        return new StatementImportResponse(total, saved, errors);
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