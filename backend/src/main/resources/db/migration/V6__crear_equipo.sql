-- RF-01: disciplina/juego principal del equipo
ALTER TABLE equipo ADD COLUMN juego_id BIGINT REFERENCES juego(id);

CREATE TABLE equipo_red_social (
                                   id        BIGSERIAL PRIMARY KEY,
                                   equipo_id BIGINT NOT NULL REFERENCES equipo(id) ON DELETE CASCADE,
                                   url       VARCHAR(300) NOT NULL
);