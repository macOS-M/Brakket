-- =====================================================================
-- Brakket - RF-12 Solicitar transferencia (EPIC-03)
-- Solicitud de transferencia de un jugador entre equipos. El flujo de
-- aprobación (jugador + capitán del equipo origen) lo completa RF-13.
-- =====================================================================

CREATE TABLE solicitud_transferencia (
    id                        BIGSERIAL PRIMARY KEY,
    jugador_id                BIGINT NOT NULL REFERENCES usuario(id),
    equipo_origen_id          BIGINT NOT NULL REFERENCES equipo(id),
    equipo_destino_id         BIGINT NOT NULL REFERENCES equipo(id),
    solicitante_id            BIGINT NOT NULL REFERENCES usuario(id),
    rol_propuesto             VARCHAR(30)  NOT NULL,
    justificacion             VARCHAR(500),
    -- PENDIENTE / APROBADA / RECHAZADA
    estado                    VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    -- Aprobaciones requeridas (RF-13): PENDIENTE / ACEPTADA / RECHAZADA
    aprobacion_jugador        VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    aprobacion_capitan_origen VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    fecha_solicitud           TIMESTAMP    NOT NULL DEFAULT now(),
    fecha_resolucion          TIMESTAMP,
    resuelta_por              BIGINT REFERENCES usuario(id)
);

-- Seguimiento por equipo solicitante y bandeja de pendientes por jugador.
CREATE INDEX idx_transferencia_destino ON solicitud_transferencia (equipo_destino_id, estado);
CREATE INDEX idx_transferencia_jugador ON solicitud_transferencia (jugador_id, estado);
CREATE INDEX idx_transferencia_origen  ON solicitud_transferencia (equipo_origen_id, estado);
