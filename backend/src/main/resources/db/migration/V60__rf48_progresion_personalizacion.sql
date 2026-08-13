ALTER TABLE logro ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE logro ADD COLUMN origen VARCHAR(180) NOT NULL DEFAULT 'Resultados y estadísticas oficiales';

ALTER TABLE logro_jugador ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE logro_jugador ADD COLUMN referencia_sistema VARCHAR(180);
ALTER TABLE logro_jugador ADD COLUMN fecha_reversion TIMESTAMP;

ALTER TABLE usuario ADD COLUMN nombre_visible_cambiado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE elemento_personalizacion (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    costo_puntos INTEGER NOT NULL CHECK (costo_puntos >= 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    logro_requerido_id BIGINT,
    CONSTRAINT fk_elemento_logro_requerido FOREIGN KEY (logro_requerido_id) REFERENCES logro(id)
);

CREATE TABLE canje_personalizacion (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    elemento_id BIGINT NOT NULL,
    costo_puntos INTEGER NOT NULL CHECK (costo_puntos >= 0),
    fecha_canje TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_canje_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_canje_elemento FOREIGN KEY (elemento_id) REFERENCES elemento_personalizacion(id),
    CONSTRAINT uq_canje_usuario_elemento UNIQUE (usuario_id, elemento_id)
);

CREATE TABLE personalizacion_perfil (
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    elemento_id BIGINT NOT NULL,
    CONSTRAINT pk_personalizacion_perfil PRIMARY KEY (usuario_id, tipo),
    CONSTRAINT fk_perfil_personalizacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_perfil_personalizacion_elemento FOREIGN KEY (elemento_id) REFERENCES elemento_personalizacion(id)
);

CREATE INDEX idx_logro_jugador_usuario_activo ON logro_jugador(usuario_id, activo);
CREATE INDEX idx_elemento_personalizacion_tipo_activo ON elemento_personalizacion(tipo, activo);
CREATE INDEX idx_canje_personalizacion_usuario ON canje_personalizacion(usuario_id);

INSERT INTO logro (nombre, descripcion, puntos_valor, activo, origen)
VALUES
    ('En equipo', 'Únete a un equipo por primera vez.', 50, TRUE, 'Membresías de equipos registradas por el sistema'),
    ('Nueva identidad', 'Cambia tu nombre visible por primera vez.', 50, TRUE, 'Historial de cambios del perfil de usuario'),
    ('Perfil con rostro', 'Agrega una foto a tu perfil.', 50, TRUE, 'Perfil de usuario registrado en el sistema'),
    ('Primera victoria', 'Gana tu primera partida registrada.', 100, TRUE, 'Resultados oficiales de partidas'),
    ('Debut competitivo', 'Participa en tu primer torneo.', 150, TRUE, 'Inscripciones y participaciones de torneos'),
    ('Fundador', 'Crea tu primer equipo.', 150, TRUE, 'Equipos creados en el sistema'),
    ('Competidor constante', 'Completa 10 partidas registradas.', 250, TRUE, 'Resultados oficiales de partidas'),
    ('Habitual de torneos', 'Participa en 5 torneos.', 300, TRUE, 'Inscripciones y participaciones de torneos'),
    ('Campeón', 'Gana un torneo registrado.', 500, TRUE, 'Resultados oficiales de torneos');

INSERT INTO elemento_personalizacion (nombre, descripcion, tipo, costo_puntos, activo, logro_requerido_id)
VALUES
    ('Estratega de Arena', 'Título para jugadores que destacan por su planificación.', 'TITULO', 100, TRUE, NULL),
    ('Fénix', 'Insignia para destacar tu perfil público.', 'INSIGNIA', 150, TRUE, NULL),
    ('Bronce', 'Marco decorativo para tu perfil público.', 'MARCO', 200, TRUE, NULL),
    ('Rival Implacable', 'Título para quienes nunca bajan la intensidad.', 'TITULO', 250, TRUE, NULL),
    ('Nova', 'Insignia luminosa para destacar tu trayectoria.', 'INSIGNIA', 300, TRUE, NULL),
    ('Neón', 'Marco vibrante para perfiles competitivos.', 'MARCO', 400, TRUE, NULL);
