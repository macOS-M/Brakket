CREATE TABLE canal_oficial_twitch (
    id                  BIGSERIAL PRIMARY KEY,
    twitch_usuario_id   VARCHAR(120),
    login_canal         VARCHAR(120) NOT NULL,
    nombre_mostrado     VARCHAR(150),
    url_canal           VARCHAR(300) NOT NULL,
    estado              VARCHAR(30) NOT NULL,
    activo              BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_error        VARCHAR(500),
    ultima_validacion   TIMESTAMP,
    creado_en           TIMESTAMP NOT NULL,
    actualizado_en      TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uq_canal_oficial_twitch_activo
    ON canal_oficial_twitch (activo) WHERE activo = TRUE;

-- Los enlaces de la transmisión siguen la convención del esquema (V1):
-- si el torneo/partida/canal se elimina, su transmisión asociada pierde
-- sentido y cae con él; sin el CASCADE el borrado de torneos revienta.
CREATE TABLE transmision_twitch (
    id                  BIGSERIAL PRIMARY KEY,
    canal_id            BIGINT NOT NULL REFERENCES canal_oficial_twitch(id) ON DELETE CASCADE,
    twitch_stream_id    VARCHAR(120),
    torneo_id           BIGINT REFERENCES torneo(id) ON DELETE CASCADE,
    partida_id          BIGINT REFERENCES partida(id) ON DELETE CASCADE,
    estado              VARCHAR(30) NOT NULL,
    iniciada_en         TIMESTAMP,
    finalizada_en       TIMESTAMP,
    creada_en           TIMESTAMP NOT NULL,
    CHECK (torneo_id IS NOT NULL OR partida_id IS NOT NULL)
);

CREATE UNIQUE INDEX uq_transmision_twitch_stream
    ON transmision_twitch (twitch_stream_id) WHERE twitch_stream_id IS NOT NULL;

-- Las métricas históricas sobreviven a la transmisión: solo pierden el enlace.
ALTER TABLE metrica_audiencia
    ADD COLUMN transmision_twitch_id BIGINT REFERENCES transmision_twitch(id) ON DELETE SET NULL;
ALTER TABLE metrica_chat
    ADD COLUMN transmision_twitch_id BIGINT REFERENCES transmision_twitch(id) ON DELETE SET NULL;

CREATE INDEX idx_metrica_aud_transmision
    ON metrica_audiencia (transmision_twitch_id, fecha_hora);
CREATE INDEX idx_metrica_chat_transmision
    ON metrica_chat (transmision_twitch_id, fecha_hora);

CREATE TABLE incidente_integracion_twitch (
    id          BIGSERIAL PRIMARY KEY,
    canal_id    BIGINT REFERENCES canal_oficial_twitch(id) ON DELETE CASCADE,
    tipo        VARCHAR(60) NOT NULL,
    detalle     VARCHAR(500) NOT NULL,
    ocurrido_en TIMESTAMP NOT NULL
);
