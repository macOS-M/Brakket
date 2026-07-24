ALTER TABLE patrocinador ADD COLUMN descripcion VARCHAR(500);
ALTER TABLE patrocinador ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

CREATE TABLE patrocinador_enlace (
                                     id              BIGSERIAL PRIMARY KEY,
                                     patrocinador_id BIGINT NOT NULL REFERENCES patrocinador(id) ON DELETE CASCADE,
                                     url             VARCHAR(300) NOT NULL
);