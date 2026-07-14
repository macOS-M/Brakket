-- RF-04: visibilidad del estado del equipo y control de actualizaciones concurrentes.
ALTER TABLE equipo ADD COLUMN estado VARCHAR(40) NOT NULL DEFAULT 'ACTIVO';
ALTER TABLE equipo ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE miembro_equipo ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_inscripcion_equipo ON inscripcion(equipo_id);
