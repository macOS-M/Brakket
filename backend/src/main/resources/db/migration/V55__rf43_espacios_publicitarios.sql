-- V55: Crea la tabla de espacios publicitarios (RF-43)
-- Cada espacio cuelga de un patrocinio y hereda su vigencia (fecha_inicio/fecha_fin
-- de patrocinio). No se duplica vigencia aquí por diseño: ver V54.

CREATE TABLE espacio_publicitario (
                                      id BIGSERIAL PRIMARY KEY,
                                      patrocinio_id BIGINT NOT NULL,
                                      ubicacion VARCHAR(30) NOT NULL,
                                      imagen_url VARCHAR(500),
                                      enlace_url VARCHAR(500),
                                      estado VARCHAR(40) NOT NULL DEFAULT 'ACTIVO',
                                      version BIGINT NOT NULL DEFAULT 0,

                                      CONSTRAINT espacio_publicitario_patrocinio_id_fkey
                                          FOREIGN KEY (patrocinio_id) REFERENCES patrocinio(id) ON DELETE CASCADE,

                                      CONSTRAINT ck_espacio_publicitario_ubicacion
                                          CHECK (ubicacion IN (
                                                               'TRANSMISION_INFERIOR',
                                                               'TORNEO_CABECERA',
                                                               'LIGA_CABECERA',
                                                               'DASHBOARD_CARD',
                                                               'CALENDARIO_FRANJA'
                                              ))
);

CREATE INDEX idx_espacio_publicitario_patrocinio_id ON espacio_publicitario(patrocinio_id);
CREATE INDEX idx_espacio_publicitario_ubicacion ON espacio_publicitario(ubicacion);