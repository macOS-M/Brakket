-- Roles base del sistema (perfiles ACT-01 a ACT-06 de la ERS)
INSERT INTO rol (nombre_rol) VALUES
    ('ADMIN'),
    ('COMISIONADO'),
    ('ARBITRO'),
    ('CAPITAN'),
    ('JUGADOR'),
    ('PATROCINADOR')
ON CONFLICT (nombre_rol) DO NOTHING;
