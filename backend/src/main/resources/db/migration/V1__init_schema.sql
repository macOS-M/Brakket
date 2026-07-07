-- =====================================================================
-- Brakket - Esquema inicial de base de datos (PostgreSQL)
-- Basado en el Diccionario de Datos del Informe de Diseño (26 entidades)
-- Fuente única de verdad del esquema. Las entidades JPA lo mapean.
-- =====================================================================

-- ---------- Autenticación y roles (EPIC-01) ----------
CREATE TABLE rol (
    id          BIGSERIAL PRIMARY KEY,
    nombre_rol  VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE usuario (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(120) NOT NULL,
    correo     VARCHAR(180) NOT NULL UNIQUE,
    google_id  VARCHAR(180) NOT NULL UNIQUE,
    foto_url   VARCHAR(500)
);

CREATE TABLE usuario_rol (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    rol_id     BIGINT NOT NULL REFERENCES rol(id),
    UNIQUE (usuario_id, rol_id)
);

-- ---------- Auditoría (RNF-04, RNF-14) ----------
CREATE TABLE log_auditoria (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuario(id),
    accion     VARCHAR(180) NOT NULL,
    entidad    VARCHAR(120) NOT NULL,
    entidad_id BIGINT NOT NULL,
    fecha      TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------- Catálogo de juegos (EPIC-06) ----------
CREATE TABLE juego (
    id      BIGSERIAL PRIMARY KEY,
    nombre  VARCHAR(120) NOT NULL UNIQUE,
    genero  VARCHAR(80)  NOT NULL,
    activo  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ---------- Progresión y logros (EPIC-13) ----------
CREATE TABLE logro (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(120) NOT NULL,
    descripcion  VARCHAR(500) NOT NULL,
    puntos_valor INT NOT NULL DEFAULT 0
);

CREATE TABLE logro_jugador (
    id               BIGSERIAL PRIMARY KEY,
    usuario_id       BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    logro_id         BIGINT NOT NULL REFERENCES logro(id),
    fecha_desbloqueo DATE   NOT NULL,
    UNIQUE (usuario_id, logro_id)
);

CREATE TABLE estadistica_jugador (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    juego_id        BIGINT NOT NULL REFERENCES juego(id),
    victorias       INT NOT NULL DEFAULT 0,
    derrotas        INT NOT NULL DEFAULT 0,
    torneos_jugados INT NOT NULL DEFAULT 0,
    UNIQUE (usuario_id, juego_id)
);

-- ---------- Equipos y plantillas (EPIC-02, 03) ----------
CREATE TABLE equipo (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(120) NOT NULL UNIQUE,
    logo        VARCHAR(500),
    descripcion VARCHAR(500),
    capitan_id  BIGINT NOT NULL REFERENCES usuario(id)
);

CREATE TABLE miembro_equipo (
    id          BIGSERIAL PRIMARY KEY,
    equipo_id   BIGINT NOT NULL REFERENCES equipo(id) ON DELETE CASCADE,
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id),
    estado      VARCHAR(40) NOT NULL,
    fecha_union DATE NOT NULL,
    UNIQUE (equipo_id, usuario_id)
);

-- ---------- Ligas y temporadas (EPIC-07) ----------
CREATE TABLE liga (
    id             BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(150) NOT NULL,
    juego_id       BIGINT NOT NULL REFERENCES juego(id),
    comisionado_id BIGINT NOT NULL REFERENCES usuario(id)
);

CREATE TABLE temporada (
    id           BIGSERIAL PRIMARY KEY,
    liga_id      BIGINT NOT NULL REFERENCES liga(id) ON DELETE CASCADE,
    nombre       VARCHAR(150) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin    DATE NOT NULL
);

-- ---------- Torneos, fixtures e inscripciones (EPIC-08) ----------
CREATE TABLE torneo (
    id           BIGSERIAL PRIMARY KEY,
    temporada_id BIGINT NOT NULL REFERENCES temporada(id) ON DELETE CASCADE,
    nombre       VARCHAR(150) NOT NULL,
    formato      VARCHAR(60)  NOT NULL,
    max_equipos  INT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin    DATE NOT NULL,
    estado       VARCHAR(40) NOT NULL
);

CREATE TABLE arbitro_torneo (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    torneo_id  BIGINT NOT NULL REFERENCES torneo(id) ON DELETE CASCADE,
    UNIQUE (usuario_id, torneo_id)
);

CREATE TABLE inscripcion (
    id              BIGSERIAL PRIMARY KEY,
    torneo_id       BIGINT NOT NULL REFERENCES torneo(id) ON DELETE CASCADE,
    equipo_id       BIGINT NOT NULL REFERENCES equipo(id),
    estado          VARCHAR(40) NOT NULL,
    fecha_solicitud DATE NOT NULL,
    UNIQUE (torneo_id, equipo_id)
);

CREATE TABLE partida (
    id          BIGSERIAL PRIMARY KEY,
    torneo_id   BIGINT NOT NULL REFERENCES torneo(id) ON DELETE CASCADE,
    equipo_a_id BIGINT NOT NULL REFERENCES equipo(id),
    equipo_b_id BIGINT NOT NULL REFERENCES equipo(id),
    ronda       INT NOT NULL,
    marcador_a  INT,
    marcador_b  INT,
    ganador_id  BIGINT REFERENCES equipo(id),
    estado      VARCHAR(40) NOT NULL
);

-- ---------- Resultados y disputas (EPIC-09) ----------
CREATE TABLE disputa (
    id            BIGSERIAL PRIMARY KEY,
    partida_id    BIGINT NOT NULL REFERENCES partida(id) ON DELETE CASCADE,
    levantada_por BIGINT NOT NULL REFERENCES usuario(id),
    arbitro_id    BIGINT REFERENCES usuario(id),
    motivo        VARCHAR(500) NOT NULL,
    evidencia_url VARCHAR(500),
    resolucion    VARCHAR(1000),
    sancion       VARCHAR(500),
    estado        VARCHAR(40) NOT NULL
);

CREATE TABLE apelacion (
    id             BIGSERIAL PRIMARY KEY,
    disputa_id     BIGINT NOT NULL REFERENCES disputa(id) ON DELETE CASCADE,
    comisionado_id BIGINT REFERENCES usuario(id),
    motivo         VARCHAR(500) NOT NULL,
    decision_final VARCHAR(1000)
);

-- ---------- Patrocinios (EPIC-11) ----------
CREATE TABLE patrocinador (
    id       BIGSERIAL PRIMARY KEY,
    nombre   VARCHAR(150) NOT NULL,
    logo     VARCHAR(500),
    contacto VARCHAR(180) NOT NULL
);

CREATE TABLE patrocinio (
    id              BIGSERIAL PRIMARY KEY,
    patrocinador_id BIGINT NOT NULL REFERENCES patrocinador(id) ON DELETE CASCADE,
    torneo_id       BIGINT NOT NULL REFERENCES torneo(id) ON DELETE CASCADE,
    nivel           VARCHAR(60) NOT NULL,
    estado          VARCHAR(40) NOT NULL
);

CREATE TABLE reporte_patrocinador (
    id               BIGSERIAL PRIMARY KEY,
    patrocinador_id  BIGINT NOT NULL REFERENCES patrocinador(id) ON DELETE CASCADE,
    torneo_id        BIGINT NOT NULL REFERENCES torneo(id),
    fecha_generacion DATE NOT NULL,
    archivo_url      VARCHAR(500) NOT NULL
);

-- ---------- Integración Twitch (EPIC-10) ----------
CREATE TABLE cuenta_twitch (
    id           BIGSERIAL PRIMARY KEY,
    equipo_id    BIGINT NOT NULL REFERENCES equipo(id) ON DELETE CASCADE,
    canal_id     VARCHAR(120) NOT NULL,
    token_acceso VARCHAR(500) NOT NULL
);

CREATE TABLE metrica_audiencia (
    id               BIGSERIAL PRIMARY KEY,
    cuenta_twitch_id BIGINT NOT NULL REFERENCES cuenta_twitch(id) ON DELETE CASCADE,
    fecha_hora       TIMESTAMP NOT NULL,
    espectadores     INT NOT NULL
);

CREATE TABLE metrica_chat (
    id                  BIGSERIAL PRIMARY KEY,
    cuenta_twitch_id    BIGINT NOT NULL REFERENCES cuenta_twitch(id) ON DELETE CASCADE,
    fecha_hora          TIMESTAMP NOT NULL,
    mensajes_por_minuto INT NOT NULL,
    usuarios_activos    INT NOT NULL
);

-- ---------- Análisis de sentimiento / IA (EPIC-10) ----------
CREATE TABLE analisis_sentimiento (
    id              BIGSERIAL PRIMARY KEY,
    metrica_chat_id BIGINT NOT NULL REFERENCES metrica_chat(id) ON DELETE CASCADE,
    fecha_hora      TIMESTAMP NOT NULL,
    clasificacion   VARCHAR(40) NOT NULL,
    puntaje         NUMERIC(5,2) NOT NULL
);

-- ---------- Notificaciones (EPIC-12) ----------
CREATE TABLE notificacion (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    tipo       VARCHAR(60) NOT NULL,
    mensaje    VARCHAR(500) NOT NULL,
    entidad    VARCHAR(120),
    entidad_id BIGINT,
    leida      BOOLEAN NOT NULL DEFAULT FALSE,
    fecha      TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------- Índices de apoyo para consultas frecuentes ----------
CREATE INDEX idx_miembro_equipo_usuario ON miembro_equipo(usuario_id);
CREATE INDEX idx_inscripcion_torneo     ON inscripcion(torneo_id);
CREATE INDEX idx_partida_torneo         ON partida(torneo_id);
CREATE INDEX idx_disputa_estado         ON disputa(estado);
CREATE INDEX idx_notificacion_usuario   ON notificacion(usuario_id, leida);
CREATE INDEX idx_metrica_aud_cuenta     ON metrica_audiencia(cuenta_twitch_id, fecha_hora);
CREATE INDEX idx_metrica_chat_cuenta    ON metrica_chat(cuenta_twitch_id, fecha_hora);
