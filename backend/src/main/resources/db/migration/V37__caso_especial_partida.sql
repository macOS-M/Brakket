CREATE TABLE caso_especial_partida (
                                       id               BIGSERIAL PRIMARY KEY,
                                       partida_id       BIGINT NOT NULL REFERENCES partida(id) ON DELETE CASCADE,
                                       tipo             VARCHAR(30) NOT NULL,
                                       justificacion    VARCHAR(500),
                                       evidencia_url    VARCHAR(500),
                                       registrado_por_id BIGINT NOT NULL REFERENCES usuario(id),
                                       fecha            TIMESTAMP NOT NULL DEFAULT now()
);