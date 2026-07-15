-- =============================================================================
-- V4: Cartões de crédito do usuário
--
-- Responsabilidade:
--   Guarda os cartões cadastrados (bandeira + últimos 4 dígitos + apelido)
--   e vincula despesas (outgoing) e jobs de importação (import_jobs) ao
--   cartão utilizado. Exclusão é soft delete (coluna active).
-- =============================================================================

CREATE TABLE IF NOT EXISTS credit_cards (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id),
    brand            VARCHAR(50)  NOT NULL,
    last_four_digits VARCHAR(4)   NOT NULL,
    nickname         VARCHAR(100),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_credit_cards_user_id ON credit_cards(user_id);

ALTER TABLE outgoing ADD COLUMN IF NOT EXISTS credit_card_id BIGINT REFERENCES credit_cards(id);
ALTER TABLE import_jobs ADD COLUMN IF NOT EXISTS credit_card_id BIGINT REFERENCES credit_cards(id);
