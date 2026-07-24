-- Solicitudes de unión (flujo inverso a la invitación): un jugador pide
-- entrar a un equipo ajeno y el capitán acepta o rechaza.
CREATE TABLE solicitud_union (
    id BIGSERIAL PRIMARY KEY,
    equipo_id BIGINT NOT NULL REFERENCES equipo(id) ON DELETE CASCADE,
    jugador_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    mensaje VARCHAR(300),
    estado VARCHAR(40) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_respuesta TIMESTAMP
);

CREATE INDEX idx_solicitud_union_equipo ON solicitud_union(equipo_id);
CREATE INDEX idx_solicitud_union_jugador ON solicitud_union(jugador_id);
