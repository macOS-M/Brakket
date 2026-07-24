-- Integridad a nivel de base para el torneo en vivo (hallazgos del review
-- del PR #26): las validaciones existen en el service, pero solo un
-- constraint frena dos transacciones simultáneas.

-- Doble clic en "Iniciar torneo" (o dos pestañas): sin esto, ambas
-- transacciones pasan el exists() y quedan dos brackets entrelazados.
CREATE UNIQUE INDEX uk_partida_slot ON partida (torneo_id, ronda, orden);

-- Doble clic en "Solicitar unirme": una sola solicitud PENDIENTE por
-- jugador y equipo (el historial de aceptadas/rechazadas no se limita).
CREATE UNIQUE INDEX uk_solicitud_pendiente
    ON solicitud_union (equipo_id, jugador_id)
    WHERE estado = 'PENDIENTE';

-- El ganador de una partida solo puede ser uno de sus dos equipos.
ALTER TABLE partida
    ADD CONSTRAINT chk_partida_ganador
    CHECK (ganador_id IS NULL OR ganador_id = equipo_a_id OR ganador_id = equipo_b_id);
