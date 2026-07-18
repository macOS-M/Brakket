CREATE TABLE formato_competitivo (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_formato_competitivo_nombre UNIQUE (nombre)
);

CREATE TABLE estadistica_juego (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    obligatoria BOOLEAN NOT NULL DEFAULT FALSE,
    activa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE perfil_competitivo_juego (
    id BIGSERIAL PRIMARY KEY,
    juego_id BIGINT NOT NULL,
    modalidad VARCHAR(255) NOT NULL,
    plantilla_minima INTEGER NOT NULL,
    plantilla_maxima INTEGER NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_perfil_competitivo_juego_juego UNIQUE (juego_id),
    CONSTRAINT fk_perfil_competitivo_juego_juego
        FOREIGN KEY (juego_id)
        REFERENCES juego(id)
        ON DELETE CASCADE,
    CONSTRAINT ck_perfil_competitivo_plantilla
        CHECK (plantilla_minima >= 1 AND plantilla_maxima >= plantilla_minima)
);

CREATE TABLE perfil_formato (
    perfil_id BIGINT NOT NULL,
    formato_id BIGINT NOT NULL,

    CONSTRAINT pk_perfil_formato PRIMARY KEY (perfil_id, formato_id),
    CONSTRAINT fk_perfil_formato_perfil
        FOREIGN KEY (perfil_id)
        REFERENCES perfil_competitivo_juego(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_perfil_formato_formato
        FOREIGN KEY (formato_id)
        REFERENCES formato_competitivo(id)
);

CREATE TABLE perfil_estadistica (
    perfil_id BIGINT NOT NULL,
    estadistica_id BIGINT NOT NULL,

    CONSTRAINT pk_perfil_estadistica PRIMARY KEY (perfil_id, estadistica_id),
    CONSTRAINT fk_perfil_estadistica_perfil
        FOREIGN KEY (perfil_id)
        REFERENCES perfil_competitivo_juego(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_perfil_estadistica_estadistica
        FOREIGN KEY (estadistica_id)
        REFERENCES estadistica_juego(id)
);
