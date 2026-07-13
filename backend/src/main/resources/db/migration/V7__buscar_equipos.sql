-- RF-05: búsqueda de equipos.
-- El estado del equipo se agrega de forma idempotente porque RF-03 (disolver
-- equipo) introduce la misma columna en su rama; la que se integre de segunda
-- no debe fallar.
ALTER TABLE equipo ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

-- Acelera la búsqueda por nombre (case-insensitive).
CREATE INDEX IF NOT EXISTS idx_equipo_nombre_lower ON equipo (LOWER(nombre));
