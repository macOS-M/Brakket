-- =====================================================================
-- Brakket - RF-18 Administrar perfil de usuario (EPIC-01)
-- "Ajustes personales": datos personales del usuario (nombre legal,
-- nacimiento, contacto y residencia) separados de la identidad pública
-- (nombre visible, avatar, biografía) que ya vivía en el perfil.
-- =====================================================================

ALTER TABLE usuario
    ADD COLUMN nombre_completo  VARCHAR(160),
    ADD COLUMN fecha_nacimiento DATE,
    ADD COLUMN telefono         VARCHAR(25),
    ADD COLUMN pais             VARCHAR(80),
    ADD COLUMN ciudad           VARCHAR(120),
    ADD COLUMN direccion        VARCHAR(255),
    ADD COLUMN codigo_postal    VARCHAR(20),
    ADD COLUMN zona_horaria     VARCHAR(64);
