-- =============================================================================
-- V1: Baseline — estado atual do schema (conforme criado pelo ddl-auto: update)
--
-- Propósito: registrar o ponto de partida para bancos já existentes.
-- Em produção (Supabase), o Flyway detecta que não há flyway_schema_history,
-- marca este script como "já executado" (baseline) e não toca nas tabelas.
-- Em ambientes novos (dev/CI), este script cria tudo do zero.
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(255),
    created_at TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS income (
    id          BIGSERIAL      PRIMARY KEY,
    value       NUMERIC(38, 2) NOT NULL,
    description VARCHAR(255)   NOT NULL,
    frequency   VARCHAR(50)    NOT NULL,
    start_date  DATE           NOT NULL,
    end_date    DATE,
    recurring   BOOLEAN,
    category    VARCHAR(255),
    user_id     BIGINT         NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP      NOT NULL
);

CREATE TABLE IF NOT EXISTS outgoing (
    id                 BIGSERIAL      PRIMARY KEY,
    value              NUMERIC(38, 2) NOT NULL,
    description        VARCHAR(255)   NOT NULL,
    start_date         DATE           NOT NULL,
    frequency          VARCHAR(50)    NOT NULL,
    duration_in_months INTEGER        NOT NULL,
    category           VARCHAR(255),
    payment_type       VARCHAR(255),
    paid               BOOLEAN        NOT NULL DEFAULT FALSE,
    user_id            BIGINT         NOT NULL REFERENCES users (id),
    created_at         TIMESTAMP      NOT NULL,
    import_hash        VARCHAR(255),
    transaction_date   DATE
);

CREATE TABLE IF NOT EXISTS investments (
    id               BIGSERIAL        PRIMARY KEY,
    value            NUMERIC(38, 2)   NOT NULL,
    type_investments VARCHAR(255)     NOT NULL,
    start_date       DATE             NOT NULL,
    end_date         DATE             NOT NULL,
    expected_return  DOUBLE PRECISION NOT NULL, -- migrado para NUMERIC(10,4) na V2
    description      VARCHAR(255),
    user_id          BIGINT           NOT NULL REFERENCES users (id),
    created_at       TIMESTAMP        NOT NULL
);

CREATE TABLE IF NOT EXISTS payment_status (
    id          BIGSERIAL  PRIMARY KEY,
    outgoing_id BIGINT     NOT NULL REFERENCES outgoing (id),
    year_month  VARCHAR(7) NOT NULL,
    paid        BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP  NOT NULL,
    updated_at  TIMESTAMP,
    CONSTRAINT uq_payment_outgoing_month UNIQUE (outgoing_id, year_month)
);
