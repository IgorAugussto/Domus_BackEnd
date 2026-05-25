package com.igorAugusto.domus.domus.messaging;

import com.igorAugusto.domus.domus.service.StatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer RabbitMQ: processa jobs de importação de extrato em background.
 *
 * ── Por que 3 chamadas separadas ao StatementService? ────────────────────────
 *
 * Cada chamada a statementService.xxx() passa pelo proxy Spring e, portanto,
 * respeita @Transactional. Se fizéssemos tudo em uma única @Transactional:
 *
 *   1. A transação ficaria aberta DURANTE a chamada HTTP ao Python (~2-30s)
 *      segurando conexão do pool desnecessariamente.
 *
 *   2. Se o processamento falhasse, o rollback reverteria até o status
 *      PROCESSING, e o bloco catch não conseguiria persistir ERROR na mesma tx
 *      (ela já está marcada para rollback).
 *
 * Ao separar em 3 transações curtas:
 *   Tx1 → marca PROCESSING (commit imediato, ~ms)
 *   Tx2 → processa e marca DONE (pode levar segundos, isolado)
 *   Tx3 → marca ERROR se Tx2 falhou (nova tx limpa)
 *
 * O Spring só consegue interceptar @Transactional quando o método é chamado
 * de FORA do bean. Por isso o consumer (classe separada) é o ponto de
 * orquestração das três transações.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatementImportConsumer {

    private final StatementService statementService;

    @RabbitListener(queues = RabbitMQConfig.IMPORT_QUEUE)
    public void handleImportMessage(StatementImportMessage message) {
        String jobId = message.getJobId();
        log.info("[RabbitMQ] Mensagem recebida → jobId={}", jobId);

        // Tx1: curta — apenas muda status para PROCESSING
        try {
            statementService.markJobProcessing(jobId);
        } catch (Exception e) {
            log.error("[RabbitMQ] Falha ao marcar job {} como PROCESSING: {}", jobId, e.getMessage());
            return; // job pode ter sumido do banco (raro) — descarta a mensagem
        }

        // Tx2: principal — chama Python, salva outgoings, marca DONE
        try {
            statementService.doProcessImportJob(jobId);
            log.info("[RabbitMQ] Job {} concluído com sucesso", jobId);
        } catch (Exception e) {
            log.error("[RabbitMQ] Falha ao processar job {}: {}", jobId, e.getMessage(), e);

            // Tx3: curta — registra o erro no banco
            statementService.markJobError(jobId, e.getMessage());
        }
    }
}
