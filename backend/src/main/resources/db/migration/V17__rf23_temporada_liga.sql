-- RF-23: configuracion completa y trazable de temporadas de liga.
ALTER TABLE liga
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE temporada
    ADD COLUMN reglas TEXT NOT NULL DEFAULT '',
    ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT 'PLANIFICADA',
    ADD COLUMN cupo_equipos INTEGER NOT NULL DEFAULT 2,
    ADD COLUMN formato_id BIGINT REFERENCES formato_competitivo(id),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE temporada
    ADD CONSTRAINT ck_temporada_fechas CHECK (fecha_inicio <= fecha_fin),
    ADD CONSTRAINT ck_temporada_cupo CHECK (cupo_equipos >= 2),
    ADD CONSTRAINT ck_temporada_estado CHECK (estado IN ('PLANIFICADA', 'ACTIVA', 'FINALIZADA', 'CANCELADA'));

CREATE UNIQUE INDEX uq_temporada_liga_nombre
    ON temporada (liga_id, lower(nombre));

CREATE INDEX idx_temporada_calendario
    ON temporada (fecha_inicio, fecha_fin);
