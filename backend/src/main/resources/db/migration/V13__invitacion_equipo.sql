CREATE TABLE invitacion_equipo (
                                   id               BIGSERIAL PRIMARY KEY,
                                   equipo_id        BIGINT NOT NULL REFERENCES equipo(id) ON DELETE CASCADE,
                                   jugador_id       BIGINT NOT NULL REFERENCES usuario(id),
                                   rol_propuesto    VARCHAR(30) NOT NULL,
                                   mensaje          VARCHAR(300),
                                   estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                                   creado_por_id    BIGINT NOT NULL REFERENCES usuario(id),
                                   fecha_creacion   TIMESTAMP NOT NULL DEFAULT now(),
                                   fecha_respuesta  TIMESTAMP
);

CREATE UNIQUE INDEX invitacion_pendiente_unica
    ON invitacion_equipo (equipo_id, jugador_id)
    WHERE estado = 'PENDIENTE';