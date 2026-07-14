-- La base de desarrollo ya registró una versión histórica distinta de V3.
-- Estas columnas se aplican en una migración nueva para conservar ese historial.
ALTER TABLE juego ADD COLUMN IF NOT EXISTS imagen_url VARCHAR(500);
ALTER TABLE juego ADD COLUMN IF NOT EXISTS descripcion VARCHAR(1000);
