-- RF-39 · Análisis de sentimiento del chat (EPIC-10).
--
-- El análisis crea muestras de chat ligadas a la TRANSMISIÓN (flujo moderno,
-- V34-V36), no a cuenta_twitch (modelo por equipo de V1, que exige equipo y
-- token y no se usa en la integración de canal oficial). Se relaja el NOT NULL
-- para permitir muestras de chat sin cuenta de equipo asociada.
ALTER TABLE metrica_chat ALTER COLUMN cuenta_twitch_id DROP NOT NULL;

-- Índice para leer la serie de sentimiento de una transmisión (RF-40) sin
-- escanear la tabla: el termómetro consulta por transmisión ordenando por fecha.
CREATE INDEX IF NOT EXISTS idx_metrica_chat_transmision
    ON metrica_chat (transmision_twitch_id);
