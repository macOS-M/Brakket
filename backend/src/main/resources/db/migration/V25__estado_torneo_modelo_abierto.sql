-- Integración del modelo abierto (V22) con el enum EstadoTorneo de develop
-- (V18): los torneos creados por el modelo abierto guardaban 'ABIERTO',
-- que en el enum corresponde a INSCRIPCION_ABIERTA.
UPDATE torneo SET estado = 'INSCRIPCION_ABIERTA' WHERE estado = 'ABIERTO';
