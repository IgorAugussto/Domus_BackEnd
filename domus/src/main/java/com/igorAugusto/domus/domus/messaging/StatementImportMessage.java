package com.igorAugusto.domus.domus.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensagem publicada no RabbitMQ para processar um job de importação.
 *
 * Intencionalmente pequena: carrega apenas o jobId.
 * Os bytes do arquivo ficam na tabela import_jobs (PostgreSQL).
 *
 * ⚠️ Nunca coloque bytes de arquivo aqui — mensagens RabbitMQ têm limite
 *    de tamanho (default 128MB) e trafegam por rede; arquivos grandes
 *    devem sempre ficar no storage (DB ou S3).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatementImportMessage {
    private String jobId;
}
