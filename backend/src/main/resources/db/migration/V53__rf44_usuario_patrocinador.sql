-- V53: Agrega la columna usuario_id a patrocinador para permitir el aislamiento
-- de acceso en RF-44 (panel comercial del patrocinador vía GET /api/sponsors/me/panel)

ALTER TABLE patrocinador
    ADD COLUMN usuario_id BIGINT;

ALTER TABLE patrocinador
    ADD CONSTRAINT patrocinador_usuario_id_fkey
        FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE SET NULL;

-- Nota: se deja nullable intencionalmente. Un patrocinador puede existir
-- (creado por un admin) antes de que se le asocie una cuenta de usuario real
-- que inicie sesión como PATROCINADOR.

CREATE INDEX idx_patrocinador_usuario_id ON patrocinador(usuario_id);