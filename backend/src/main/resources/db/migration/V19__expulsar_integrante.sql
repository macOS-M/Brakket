-- RF-10: expulsar integrante de la plantilla (baja logica con trazabilidad).
-- La baja conserva la fila del miembro con estado EXPULSADO y registra
-- fecha, causa y responsable de la expulsion.

ALTER TABLE miembro_equipo ADD COLUMN IF NOT EXISTS fecha_baja TIMESTAMP;
ALTER TABLE miembro_equipo ADD COLUMN IF NOT EXISTS causa_baja VARCHAR(500);
ALTER TABLE miembro_equipo ADD COLUMN IF NOT EXISTS responsable_baja_id BIGINT REFERENCES usuario(id);
