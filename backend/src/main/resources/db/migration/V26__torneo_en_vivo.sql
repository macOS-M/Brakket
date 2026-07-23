-- Torneo en vivo: bracket de eliminación directa, partidas con lobby y
-- reporte de resultados (RF-26/27/29 en su versión mínima demoable).

-- Ajustes de partida que el organizador define al crear el torneo (pares
-- clave/valor serializados como JSON): el contrato que ambos capitanes
-- deben aplicar al crear la partida privada dentro del juego.
ALTER TABLE torneo ADD COLUMN ajustes_partida TEXT;

-- Campeón del torneo (se fija al confirmar la final).
ALTER TABLE torneo ADD COLUMN campeon_equipo_id BIGINT REFERENCES equipo(id) ON DELETE SET NULL;

-- Identidad del capitán dentro del juego (gamertag): sin API oficial del
-- juego es lo que "calza" la cuenta de Brakket con quien aparece en la
-- lobby. Nullable por las inscripciones previas a esta migración.
ALTER TABLE inscripcion ADD COLUMN usuario_en_juego VARCHAR(100);

-- La tabla partida existe desde V1 pero el motor de competencias nunca se
-- construyó. Se adapta al bracket: equipos nullable (los slots se llenan
-- al avanzar la llave y los byes no tienen rival), posición dentro de la
-- ronda, lobby de partida privada y enlace de avance.
ALTER TABLE partida ALTER COLUMN equipo_a_id DROP NOT NULL;
ALTER TABLE partida ALTER COLUMN equipo_b_id DROP NOT NULL;
ALTER TABLE partida ADD COLUMN orden INT NOT NULL DEFAULT 0;
ALTER TABLE partida ADD COLUMN reportado_por_equipo_id BIGINT REFERENCES equipo(id) ON DELETE SET NULL;
ALTER TABLE partida ADD COLUMN lobby_nombre VARCHAR(80);
ALTER TABLE partida ADD COLUMN lobby_clave VARCHAR(40);
ALTER TABLE partida ADD COLUMN siguiente_partida_id BIGINT REFERENCES partida(id) ON DELETE SET NULL;
