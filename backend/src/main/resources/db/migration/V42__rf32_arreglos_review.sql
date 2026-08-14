-- RF-32, arreglos del review: una disputa solo puede tener una apelación
-- activa. El índice único es la barrera real contra 2 apelaciones
-- simultáneas (el chequeo de la aplicación por sí solo no alcanza), y de
-- paso deja indexado disputa_id para las consultas de listar/verificar
-- (antes no tenía ningún índice).
CREATE UNIQUE INDEX idx_apelacion_disputa_unica ON apelacion(disputa_id);