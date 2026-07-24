-- Módulo de equipos ampliado (referencia Challenger Mode): banner de
-- portada, sitio web y video de presentación. Aditiva: nada se rompe
-- para filas existentes.
ALTER TABLE equipo ADD COLUMN banner_url VARCHAR(500);
ALTER TABLE equipo ADD COLUMN sitio_web VARCHAR(500);
ALTER TABLE equipo ADD COLUMN video_url VARCHAR(500);
