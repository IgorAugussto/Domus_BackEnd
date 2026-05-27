package com.igorAugusto.domus.domus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igorAugusto.domus.domus.dto.ImportJobStatusResponse;
import com.igorAugusto.domus.domus.dto.StatementImportResponse;
import com.igorAugusto.domus.domus.entity.ImportJob;
import com.igorAugusto.domus.domus.entity.Outgoing;
import com.igorAugusto.domus.domus.entity.User;
import com.igorAugusto.domus.domus.enums.Frequency;
import com.igorAugusto.domus.domus.enums.ImportJobStatus;
import com.igorAugusto.domus.domus.exception.ForbiddenException;
import com.igorAugusto.domus.domus.exception.ResourceNotFoundException;
import com.igorAugusto.domus.domus.messaging.StatementImportProducer;
import com.igorAugusto.domus.domus.repository.ImportJobRepository;
import com.igorAugusto.domus.domus.repository.OutgoingRepository;
import com.igorAugusto.domus.domus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final OutgoingRepository outgoingRepository;
    private final UserRepository userRepository;
    private final ImportJobRepository importJobRepository;
    private final StatementImportProducer statementImportProducer;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // injetado pelo Spring Boot (já configurado)

    @Value("${statement.service.url:http://localhost:5000}")
    private String statementServiceUrl;

    // ════════════════════════════════════════════════════════════════════════
    // 1. ENQUEUE — Controller chama aqui. Rápido: persiste job + publica Rabbit
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Persiste o arquivo no banco, cria o job com status PENDING e publica
     * o jobId no RabbitMQ. Retorna o jobId imediatamente (não bloqueia o usuário).
     */
    @Transactional
    public String enqueueImport(MultipartFile file, String dueDate, String userEmail) {
        try {
            String jobId = UUID.randomUUID().toString();

            ImportJob job = ImportJob.builder()
                    .id(jobId)
                    .userEmail(userEmail)
                    .dueDate(dueDate)
                    .fileBytes(file.getBytes())
                    .originalFilename(file.getOriginalFilename())
                    .status(ImportJobStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            importJobRepository.save(job);

            // Publica no RabbitMQ somente após o commit da transação.
            // Se publicado dentro da tx, o consumer pode receber a mensagem antes
            // do commit e não encontrar o job no banco (race condition).
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    statementImportProducer.sendImportJob(jobId);
                }
            });

            log.info("[Statement] Job enfileirado → jobId={}, user={}, arquivo={}",
                    jobId, userEmail, file.getOriginalFilename());

            return jobId;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enfileirar importação: " + e.getMessage(), e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. PROCESS — Chamado pelo StatementImportConsumer (3 etapas separadas)
    //    Cada método abaixo é @Transactional e chamado de FORA desta classe,
    //    garantindo que o proxy Spring intercepte e abra/feche a transação.
    // ════════════════════════════════════════════════════════════════════════

    /** Etapa 1 (Tx curta): marca o job como PROCESSING. */
    @Transactional
    public void markJobProcessing(String jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job não encontrado: " + jobId));
        job.setStatus(ImportJobStatus.PROCESSING);
        job.setUpdatedAt(LocalDateTime.now());
        importJobRepository.save(job);
        log.info("[Statement] Job {} → PROCESSING", jobId);
    }

    /**
     * Etapa 2 (Tx principal): chama o Python, processa as transações,
     * salva os outgoings e marca o job como DONE.
     *
     * Se qualquer passo falhar, a transação faz rollback (outgoings não
     * são salvos) e o caller (Consumer) captura a exceção e chama markJobError.
     */
    @Transactional
    public void doProcessImportJob(String jobId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job não encontrado: " + jobId));

        User user = userRepository.findByEmail(job.getUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário não encontrado: " + job.getUserEmail()));

        LocalDate startDate     = LocalDate.parse(job.getDueDate());
        JsonNode pythonResponse = callPythonServiceWithBytes(job.getFileBytes(), job.getOriginalFilename());
        JsonNode transactions   = pythonResponse.get("transacoes");
        int total               = pythonResponse.get("total").asInt();

        List<String> errors  = new ArrayList<>();
        int saved   = 0;
        int skipped = 0;

        List<Outgoing> existingMonthlyOutgoings =
                outgoingRepository.findByUserIdAndFrequency(user.getId(), Frequency.MONTHLY);
        YearMonth importMonth = YearMonth.from(startDate);

        for (JsonNode transaction : transactions) {
            try {
                String description   = transaction.get("description").asText();
                double amount        = transaction.get("amount").asDouble();
                String category      = transaction.get("category").asText();
                String frequencyStr  = transaction.get("frequency").asText();
                String paymentType   = transaction.get("paymentType").asText();
                boolean paid         = transaction.get("paid").asBoolean();
                int durationInMonths = transaction.get("durationInMonths").asInt(1);

                Frequency frequency;
                try {
                    frequency = Frequency.fromValue(frequencyStr);
                } catch (IllegalArgumentException e) {
                    errors.add("Frequência inválida para '" + description + "': " + frequencyStr);
                    continue;
                }

                String transactionDateStr = transaction.has("startDate")
                        ? transaction.get("startDate").asText() : "";
                LocalDate transactionDate = null;
                try {
                    if (!transactionDateStr.isBlank()) {
                        transactionDate = LocalDate.parse(transactionDateStr);
                    }
                } catch (Exception ignored) {}

                String hashInput = user.getId() + "|" + transactionDateStr + "|"
                        + description.toLowerCase().trim() + "|"
                        + String.format("%.2f", amount);
                String importHash = DigestUtils.md5DigestAsHex(
                        hashInput.getBytes(StandardCharsets.UTF_8));

                if (outgoingRepository.existsByImportHash(importHash)) {
                    skipped++;
                    continue;
                }

                if (Frequency.MONTHLY.equals(frequency)) {
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

                Outgoing savedOutgoing = outgoingRepository.save(outgoing);
                if (Frequency.MONTHLY.equals(frequency)) {
                    existingMonthlyOutgoings.add(savedOutgoing);
                }
                saved++;

            } catch (Exception e) {
                errors.add("Erro ao salvar transação: " + e.getMessage());
            }
        }

        // Persiste o resultado e marca como DONE (ainda na mesma transação)
        try {
            StatementImportResponse result = new StatementImportResponse(total, saved, skipped, errors);
            job.setStatus(ImportJobStatus.DONE);
            job.setResultJson(objectMapper.writeValueAsString(result));
            job.setUpdatedAt(LocalDateTime.now());
            importJobRepository.save(job);
            log.info("[Statement] Job {} → DONE (saved={}, skipped={}, errors={})",
                    jobId, saved, skipped, errors.size());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao finalizar job: " + e.getMessage(), e);
        }
    }

    /** Etapa 3 (Tx curta, chamada pelo consumer em caso de erro): marca como ERROR. */
    @Transactional
    public void markJobError(String jobId, String errorMessage) {
        try {
            ImportJob job = importJobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job não encontrado: " + jobId));
            String safeMsg = errorMessage != null
                    ? errorMessage.replace("\"", "'").replace("\n", " ")
                    : "Erro desconhecido";
            job.setStatus(ImportJobStatus.ERROR);
            job.setResultJson("{\"error\":\"" + safeMsg + "\"}");
            job.setUpdatedAt(LocalDateTime.now());
            importJobRepository.save(job);
            log.error("[Statement] Job {} → ERROR: {}", jobId, safeMsg);
        } catch (Exception e) {
            log.error("[Statement] Não foi possível marcar job {} como ERROR: {}", jobId, e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. STATUS — Polling do frontend via GET /api/statement/status/{jobId}
    // ════════════════════════════════════════════════════════════════════════

    public ImportJobStatusResponse getJobStatus(String jobId, String userEmail) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job de importação não encontrado"));

        // Segurança: usuário só pode consultar seus próprios jobs
        if (!job.getUserEmail().equals(userEmail)) {
            throw new ForbiddenException("Acesso negado ao job de importação");
        }

        if (job.getStatus() == ImportJobStatus.DONE && job.getResultJson() != null) {
            try {
                StatementImportResponse result =
                        objectMapper.readValue(job.getResultJson(), StatementImportResponse.class);
                return new ImportJobStatusResponse(job.getStatus().name(), result);
            } catch (Exception e) {
                log.error("[Statement] Erro ao deserializar resultado do job {}: {}", jobId, e.getMessage());
            }
        }

        return new ImportJobStatusResponse(job.getStatus().name(), null);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVADO: chama o microserviço Python com os bytes do arquivo
    // ════════════════════════════════════════════════════════════════════════

    private JsonNode callPythonServiceWithBytes(byte[] fileBytes, String filename) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filename;
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
            throw new RuntimeException("Erro ao comunicar com o serviço Python: " + e.getMessage(), e);
        }
    }
}
