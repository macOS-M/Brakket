-- RF-32: resolución de la disputa por un árbitro/comisionado, y
-- apelación posterior ante el comisionado.
ALTER TABLE disputa ADD COLUMN decision VARCHAR(20);
ALTER TABLE disputa ADD COLUMN justificacion_resolucion VARCHAR(1000);
-- La columna sancion ya existía desde el esquema original (V1).
ALTER TABLE disputa ADD COLUMN resuelta_por_id BIGINT REFERENCES usuario(id);
ALTER TABLE disputa ADD COLUMN fecha_resolucion TIMESTAMP;

ALTER TABLE apelacion ADD COLUMN apelada_por_id BIGINT REFERENCES usuario(id);
ALTER TABLE apelacion ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';
ALTER TABLE apelacion ADD COLUMN fecha_creacion TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE apelacion ADD COLUMN fecha_resolucion TIMESTAMP;