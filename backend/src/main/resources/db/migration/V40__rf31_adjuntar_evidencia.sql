-- RF-31: evidencia adjunta a una disputa (puede haber varias, de
-- distintas personas, a lo largo del tiempo).
CREATE TABLE evidencia_disputa (
                                   id             BIGSERIAL PRIMARY KEY,
                                   disputa_id     BIGINT NOT NULL REFERENCES disputa(id) ON DELETE CASCADE,
                                   subido_por_id  BIGINT NOT NULL REFERENCES usuario(id),
                                   url            VARCHAR(500) NOT NULL,
                                   descripcion    VARCHAR(500),
                                   fecha_creacion TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_evidencia_disputa ON evidencia_disputa(disputa_id);