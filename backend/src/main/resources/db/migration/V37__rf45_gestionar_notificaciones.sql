ALTER TABLE notificacion
    ADD COLUMN estado_entrega VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    ADD COLUMN origen VARCHAR(120),
    ADD COLUMN eliminada_bandeja BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE notificacion
SET origen = COALESCE(entidad, 'Sistema')
WHERE origen IS NULL;

ALTER TABLE notificacion ALTER COLUMN origen SET NOT NULL;

CREATE INDEX idx_notificacion_bandeja
    ON notificacion(usuario_id, eliminada_bandeja, fecha DESC);

CREATE UNIQUE INDEX uq_notificacion_evento
    ON notificacion(usuario_id, tipo, entidad, entidad_id, md5(mensaje))
    WHERE entidad_id IS NOT NULL;
