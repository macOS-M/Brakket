-- RF-47: el selector remoto usa búsquedas parciales por nombre. Los índices
-- trigram evitan recorridos completos cuando crezcan jugadores y equipos.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_usuario_nombre_trgm
    ON usuario USING gin (lower(nombre) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_equipo_nombre_trgm
    ON equipo USING gin (lower(nombre) gin_trgm_ops);

-- Solo las partidas oficiales alimentan el historial y sus selectores.
CREATE INDEX IF NOT EXISTS idx_partida_oficial_equipo_a
    ON partida (equipo_a_id)
    WHERE estado = 'FINALIZADA' AND equipo_a_id IS NOT NULL AND equipo_b_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_partida_oficial_equipo_b
    ON partida (equipo_b_id)
    WHERE estado = 'FINALIZADA' AND equipo_a_id IS NOT NULL AND equipo_b_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_torneo_juego_fecha
    ON torneo (juego_id, fecha_inicio);
