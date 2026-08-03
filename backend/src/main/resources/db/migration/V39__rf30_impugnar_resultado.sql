-- RF-30: impugnar un resultado ya finalizado.
-- La partida necesita saber CUÁNDO se finalizó para poder calcular el
-- plazo de impugnación (48h) desde el backend.
ALTER TABLE partida ADD COLUMN fecha_finalizacion TIMESTAMP;

-- La disputa ya existía desde el esquema inicial, pero le faltaban estos
-- dos datos que pide RF-30: cuándo se creó (para el plazo de apelación
-- más adelante) y la descripción larga (separada del motivo corto).
ALTER TABLE disputa ADD COLUMN fecha_creacion TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE disputa ADD COLUMN descripcion VARCHAR(1000);