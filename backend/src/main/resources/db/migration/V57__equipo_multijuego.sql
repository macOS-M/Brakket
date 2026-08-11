-- Un equipo puede competir en varias disciplinas. juego_id se conserva como
-- juego principal para compatibilidad con búsquedas y clientes existentes.
CREATE TABLE equipo_juego (
    equipo_id BIGINT NOT NULL REFERENCES equipo(id) ON DELETE CASCADE,
    juego_id BIGINT NOT NULL REFERENCES juego(id),
    PRIMARY KEY (equipo_id, juego_id)
);

INSERT INTO equipo_juego (equipo_id, juego_id)
SELECT id, juego_id FROM equipo WHERE juego_id IS NOT NULL
ON CONFLICT DO NOTHING;

CREATE INDEX idx_equipo_juego_juego ON equipo_juego(juego_id);
