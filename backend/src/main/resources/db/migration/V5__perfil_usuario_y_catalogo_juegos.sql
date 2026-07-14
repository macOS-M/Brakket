-- Perfil extendido para usuarios y catálogo inicial de juegos.

ALTER TABLE usuario
    ADD COLUMN biografia TEXT,
    ADD COLUMN redes_sociales TEXT,
    ADD COLUMN visibilidad_perfil VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

CREATE TABLE usuario_juego_preferido (
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    juego_id   BIGINT NOT NULL REFERENCES juego(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, juego_id)
);

INSERT INTO juego (nombre, genero, activo) VALUES
    ('League of Legends', 'MOBA', TRUE),
    ('Valorant', 'Shooter táctico', TRUE),
    ('Counter-Strike 2', 'Shooter táctico', TRUE),
    ('Rocket League', 'Deportes / arcade', TRUE),
    ('EA Sports FC 25', 'Deportes', TRUE)
ON CONFLICT (nombre) DO NOTHING;