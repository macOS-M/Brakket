-- Motores de formato más allá de la eliminación directa (cierra DD-05):
-- doble eliminación, round robin, suizo y fase de grupos + eliminación.

-- Sección de la competencia (WINNERS/LOSERS/GRAN_FINAL/GRUPOS/ELIMINACION);
-- null en los formatos de una sola estructura.
ALTER TABLE partida ADD COLUMN fase VARCHAR(20);

-- Índice del grupo (0-based) para las partidas de fase de grupos.
ALTER TABLE partida ADD COLUMN grupo INT;

-- Slot explícito que ocupa el ganador en su siguiente partida ('A'/'B').
-- Las filas viejas lo dejan null y el motor cae al criterio orden % 2.
ALTER TABLE partida ADD COLUMN siguiente_slot VARCHAR(1);

-- Enlace de descenso de la doble eliminación: a qué partida de la llave
-- inferior cae el perdedor, y en qué slot.
ALTER TABLE partida ADD COLUMN perdedor_siguiente_partida_id BIGINT REFERENCES partida(id) ON DELETE SET NULL;
ALTER TABLE partida ADD COLUMN perdedor_slot VARCHAR(1);

-- El slot (ronda, orden) ya no es único por torneo: la llave inferior y
-- cada grupo repiten numeración. La unicidad ahora incluye fase y grupo.
DROP INDEX uk_partida_slot;
CREATE UNIQUE INDEX uk_partida_slot
    ON partida (torneo_id, COALESCE(fase, ''), COALESCE(grupo, -1), ronda, orden);
