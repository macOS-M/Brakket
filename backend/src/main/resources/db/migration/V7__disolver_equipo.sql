-- RF-03: disolución lógica de equipos.
-- El equipo nunca se borra: cambia a estado DISUELTO conservando su historial,
-- y se registra fecha, responsable y motivo (opcional) de la disolución.
ALTER TABLE equipo ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';
ALTER TABLE equipo ADD COLUMN fecha_disolucion TIMESTAMP;
ALTER TABLE equipo ADD COLUMN motivo_disolucion VARCHAR(500);
ALTER TABLE equipo ADD COLUMN disuelto_por BIGINT REFERENCES usuario(id);
