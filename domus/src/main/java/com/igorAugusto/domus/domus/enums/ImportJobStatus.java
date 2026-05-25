package com.igorAugusto.domus.domus.enums;

/**
 * Estados possíveis de um job de importação de extrato.
 *
 * PENDING    → job criado, aguardando consumer do RabbitMQ
 * PROCESSING → consumer pegou a mensagem, processamento em andamento
 * DONE       → processamento concluído com sucesso
 * ERROR      → falha durante o processamento (Python indisponível, arquivo inválido, etc.)
 */
public enum ImportJobStatus {
    PENDING,
    PROCESSING,
    DONE,
    ERROR
}
