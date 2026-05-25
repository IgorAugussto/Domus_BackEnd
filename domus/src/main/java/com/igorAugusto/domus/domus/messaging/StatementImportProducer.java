package com.igorAugusto.domus.domus.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica mensagens de importação de extrato no RabbitMQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatementImportProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Envia o jobId para a fila de importação.
     * O consumer vai buscar os bytes do arquivo diretamente do banco pelo jobId.
     *
     * @param jobId UUID do ImportJob já persistido no banco
     */
    public void sendImportJob(String jobId) {
        StatementImportMessage message = new StatementImportMessage(jobId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.IMPORT_EXCHANGE,
                RabbitMQConfig.IMPORT_ROUTING_KEY,
                message
        );
        log.info("[RabbitMQ] Mensagem publicada → exchange={}, routingKey={}, jobId={}",
                RabbitMQConfig.IMPORT_EXCHANGE, RabbitMQConfig.IMPORT_ROUTING_KEY, jobId);
    }
}
