-- RF-24/RF-25 con modelo abierto de organizadores (decisión de diseño
-- inspirada en Challenger Mode; ver docs/decisiones-diseno.md):
--   * Un torneo puede colgar directo del juego (comunitario) o de una
--     temporada de liga; temporada_id pasa a ser opcional.
--   * Quien crea el torneo queda como su organizador (dueño contextual).
--   * publico controla si se lista y acepta inscripciones abiertas.
--   * tamano_equipo es el "5v5" del torneo; max_equipos ya existía (cupo).
--   * Las fechas pasan a timestamp: los torneos arrancan a una hora.

ALTER TABLE torneo
    ALTER COLUMN temporada_id DROP NOT NULL,
    ADD COLUMN juego_id       BIGINT REFERENCES juego(id),
    ADD COLUMN organizador_id BIGINT REFERENCES usuario(id),
    ADD COLUMN publico        BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN tamano_equipo  INT NOT NULL DEFAULT 5,
    ADD COLUMN descripcion    VARCHAR(1000);

ALTER TABLE torneo ALTER COLUMN fecha_inicio TYPE TIMESTAMP USING fecha_inicio::timestamp;
ALTER TABLE torneo ALTER COLUMN fecha_fin DROP NOT NULL;
ALTER TABLE torneo ALTER COLUMN fecha_fin TYPE TIMESTAMP USING fecha_fin::timestamp;

-- Backfill defensivo para filas previas (en dev la tabla está vacía):
-- el juego y el organizador se derivan de la liga de su temporada.
UPDATE torneo
SET juego_id = sub.juego_id
FROM (SELECT t.id AS temporada_id, l.juego_id
      FROM temporada t JOIN liga l ON l.id = t.liga_id) sub
WHERE torneo.temporada_id = sub.temporada_id AND torneo.juego_id IS NULL;

UPDATE torneo
SET organizador_id = sub.comisionado_id
FROM (SELECT t.id AS temporada_id, l.comisionado_id
      FROM temporada t JOIN liga l ON l.id = t.liga_id) sub
WHERE torneo.temporada_id = sub.temporada_id AND torneo.organizador_id IS NULL;

ALTER TABLE torneo ALTER COLUMN juego_id SET NOT NULL;
ALTER TABLE torneo ALTER COLUMN organizador_id SET NOT NULL;
