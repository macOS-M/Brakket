-- RF-05: búsqueda de equipos.
-- La columna estado la introduce RF-03 (V7__disolver_equipo, que se integra
-- antes según el orden de merge acordado). El ALTER idempotente se conserva
-- solo como red de seguridad para poder correr esta rama de forma aislada;
-- con RF-03 ya aplicado es un no-op.
ALTER TABLE equipo ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

-- Acelera la búsqueda por nombre (case-insensitive).
CREATE INDEX IF NOT EXISTS idx_equipo_nombre_lower ON equipo (LOWER(nombre));
