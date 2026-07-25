-- RF-04: perfil público de equipo.
-- La columna estado la introduce RF-03 (V7__disolver_equipo, que se integra
-- antes según el orden de merge). El IF NOT EXISTS queda solo como red de
-- seguridad para poder correr esta rama de forma aislada; con RF-03 aplicado
-- es un no-op.
ALTER TABLE equipo ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

-- Acelera la carga de torneos del perfil (inscripciones por equipo).
CREATE INDEX IF NOT EXISTS idx_inscripcion_equipo ON inscripcion(equipo_id);
