-- =====================================================================
-- Brakket - RF-14 Registrar transferencia en historial (EPIC-04/05)
-- Registro histórico e inmutable, generado automáticamente al completarse
-- una transferencia (RF-12/RF-13). Sirve de fuente para RF-16 (consultar
-- historial de un equipo).
-- =====================================================================

CREATE TABLE historial_transferencia (
                                         id                    BIGSERIAL PRIMARY KEY,
                                         solicitud_id          BIGINT NOT NULL UNIQUE REFERENCES solicitud_transferencia(id),
                                         jugador_id            BIGINT NOT NULL REFERENCES usuario(id),
                                         equipo_origen_id      BIGINT NOT NULL REFERENCES equipo(id),
                                         equipo_destino_id     BIGINT NOT NULL REFERENCES equipo(id),
                                         rol_asignado          VARCHAR(30) NOT NULL,
                                         responsable_id        BIGINT NOT NULL REFERENCES usuario(id),
                                         fecha_transferencia   TIMESTAMP   NOT NULL DEFAULT now()
);

-- Consultas por equipo (RF-16: historial de un equipo, tanto lo que
-- ganó como lo que perdió).
CREATE INDEX idx_historial_origen  ON historial_transferencia (equipo_origen_id);
CREATE INDEX idx_historial_destino ON historial_transferencia (equipo_destino_id);
CREATE INDEX idx_historial_jugador ON historial_transferencia (jugador_id);