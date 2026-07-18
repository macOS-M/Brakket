INSERT INTO formato_competitivo (nombre, activo) VALUES
    ('ELIMINACION_DIRECTA', TRUE),
    ('DOBLE_ELIMINACION', TRUE),
    ('ROUND_ROBIN', TRUE),
    ('SUIZO', TRUE),
    ('FASE_GRUPOS_Y_ELIMINACION', TRUE)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO estadistica_juego (nombre, obligatoria, activa)
SELECT 'PARTIDAS_JUGADAS', TRUE, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM estadistica_juego WHERE nombre = 'PARTIDAS_JUGADAS'
);

INSERT INTO estadistica_juego (nombre, obligatoria, activa)
SELECT 'VICTORIAS', TRUE, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM estadistica_juego WHERE nombre = 'VICTORIAS'
);

INSERT INTO estadistica_juego (nombre, obligatoria, activa)
SELECT 'DERROTAS', TRUE, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM estadistica_juego WHERE nombre = 'DERROTAS'
);
