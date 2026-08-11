-- V56: Bloqueante de review RF-44 — usuario_id no tenia constraint unico real,
-- solo un indice comun (V53). El codigo asumia unicidad (findByUsuarioId
-- devuelve Optional), pero nada en la base lo garantizaba. Se corrige con un
-- indice unico parcial: parcial porque usuario_id sigue siendo nullable a
-- proposito (un patrocinador puede existir antes de tener cuenta vinculada).

DROP INDEX IF EXISTS idx_patrocinador_usuario_id;

CREATE UNIQUE INDEX uq_patrocinador_usuario ON patrocinador(usuario_id) WHERE usuario_id IS NOT NULL;