-- =============================================================================
-- V2: Converte expected_return de DOUBLE PRECISION para NUMERIC(10, 4)
--
-- Motivo: DOUBLE PRECISION tem imprecisão de ponto flutuante (IEEE 754).
-- NUMERIC(10,4) garante precisão exata para percentuais financeiros.
-- Ex: 5.7% armazenado como DOUBLE pode virar 5.6999999999...
--
-- O USING faz a conversão dos dados existentes sem perda de informação.
-- =============================================================================

ALTER TABLE investments
    ALTER COLUMN expected_return TYPE NUMERIC(10, 4)
        USING expected_return::NUMERIC(10, 4);
