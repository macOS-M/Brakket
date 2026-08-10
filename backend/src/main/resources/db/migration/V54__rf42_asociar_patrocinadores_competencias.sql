-- V54: Amplía patrocinio para soportar asociación con liga, temporada o torneo,
-- con vigencia, condiciones y optimistic locking (RF-42)

-- 1. Hacer torneo_id nullable (antes era obligatorio)
ALTER TABLE patrocinio
    ALTER COLUMN torneo_id DROP NOT NULL;

-- 2. Agregar liga_id y temporada_id
ALTER TABLE patrocinio
    ADD COLUMN liga_id BIGINT,
    ADD COLUMN temporada_id BIGINT;

ALTER TABLE patrocinio
    ADD CONSTRAINT patrocinio_liga_id_fkey
        FOREIGN KEY (liga_id) REFERENCES liga(id) ON DELETE CASCADE;

ALTER TABLE patrocinio
    ADD CONSTRAINT patrocinio_temporada_id_fkey
        FOREIGN KEY (temporada_id) REFERENCES temporada(id) ON DELETE CASCADE;

-- 3. Agregar vigencia (fecha_inicio, fecha_fin) siguiendo el mismo patrón que temporada
ALTER TABLE patrocinio
    ADD COLUMN fecha_inicio DATE NOT NULL DEFAULT CURRENT_DATE,
    ADD COLUMN fecha_fin DATE NOT NULL DEFAULT CURRENT_DATE;

-- Se quitan los defaults después de la migración inicial, ya que son solo
-- para no romper filas existentes (la tabla está vacía en este punto, pero
-- se deja como buena práctica ante futuros seeds)
ALTER TABLE patrocinio
    ALTER COLUMN fecha_inicio DROP DEFAULT,
ALTER COLUMN fecha_fin DROP DEFAULT;

ALTER TABLE patrocinio
    ADD CONSTRAINT ck_patrocinio_fechas CHECK (fecha_inicio <= fecha_fin);

-- 4. Agregar condiciones (texto libre, según lo definido)
ALTER TABLE patrocinio
    ADD COLUMN condiciones VARCHAR(500);

-- 5. Optimistic locking, consistente con el patrón ya usado en temporada
ALTER TABLE patrocinio
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 6. Constraint de alcance único: el patrocinio debe apuntar a exactamente
-- una de las tres competencias posibles (liga, temporada o torneo)
ALTER TABLE patrocinio
    ADD CONSTRAINT ck_patrocinio_alcance_unico
        CHECK (num_nonnulls(liga_id, temporada_id, torneo_id) = 1);