-- =============================================================================
-- V3: Tabela de jobs de importação de extrato (processamento assíncrono via RabbitMQ)
--
-- Responsabilidade:
--   Armazena o arquivo enviado pelo usuário (bytea) e o estado do processamento.
--   O RabbitMQ carrega apenas o jobId — os bytes ficam aqui para o consumer buscar.
--
-- Ciclo de vida do status:
--   PENDING → PROCESSING → DONE
--                        → ERROR
-- =============================================================================

CREATE TABLE IF NOT EXISTS import_jobs (
    id                VARCHAR(36)  PRIMARY KEY,                 -- UUID gerado pelo Java
    user_email        VARCHAR(255) NOT NULL,
    due_date          VARCHAR(10)  NOT NULL,                    -- "YYYY-MM-DD"
    file_bytes        BYTEA        NOT NULL,                    -- bytes brutos do arquivo
    original_filename VARCHAR(255),
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING|PROCESSING|DONE|ERROR
    result_json       TEXT,                                     -- StatementImportResponse JSON ou {"error":"..."}
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP
);

-- Índices para consultas frequentes
CREATE INDEX IF NOT EXISTS idx_import_jobs_user_email ON import_jobs(user_email);
CREATE INDEX IF NOT EXISTS idx_import_jobs_status     ON import_jobs(status);
