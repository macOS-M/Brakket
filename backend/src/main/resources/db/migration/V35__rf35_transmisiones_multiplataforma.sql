-- RF-35: registro de transmisiones que alimenta la página /transmisiones.
--
-- Fuente de verdad (para no repetir la dualidad CanalOficialTwitch/CuentaTwitch):
--   * canal_oficial_twitch → SOLO la configuración del canal oficial (RF-34).
--   * transmision_twitch   → registro de transmisiones a mostrar (RF-35) y ancla
--     de las métricas (RF-36, vía metrica_audiencia/metrica_chat). Se extiende
--     esta tabla en lugar de crear una paralela para que la UI lea de la MISMA
--     fila contra la que se escribirán las métricas.
--
-- El estado vivo (título, espectadores, thumbnail, categoría) NO se persiste
-- aquí: viene de Helix y se cachea en memoria; su historial pertenece a
-- metrica_audiencia cuando llegue RF-36.

ALTER TABLE transmision_twitch
    ADD COLUMN plataforma    VARCHAR(20) NOT NULL DEFAULT 'TWITCH'
        CONSTRAINT ck_transmision_plataforma CHECK (plataforma IN ('TWITCH', 'YOUTUBE', 'KICK')),
    ADD COLUMN login_canal   VARCHAR(120),
    ADD COLUMN destacada     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN activa        BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN creada_por    BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    ADD COLUMN verificada_en TIMESTAMP;

-- Una transmisión de YouTube/Kick no tendrá canal oficial de Twitch asociado.
ALTER TABLE transmision_twitch ALTER COLUMN canal_id DROP NOT NULL;

-- V34 exigía torneo o partida; una transmisión destacada del canal (sin torneo
-- todavía) ahora es válida. El nombre es el autogenerado por Postgres para el
-- único CHECK sin nombre que declaró V34.
ALTER TABLE transmision_twitch DROP CONSTRAINT transmision_twitch_check;

-- Toda fila de Twitch debe poder resolverse a un canal consultable en Helix.
ALTER TABLE transmision_twitch
    ADD CONSTRAINT ck_transmision_twitch_canal
        CHECK (plataforma <> 'TWITCH' OR canal_id IS NOT NULL OR login_canal IS NOT NULL);

-- La página consulta solo las activas; índice parcial igual que en V34.
CREATE INDEX idx_transmision_twitch_activa
    ON transmision_twitch (activa) WHERE activa = TRUE;
