-- RF-48: dos cosméticos más para la tienda.
-- Van acá y no dentro de V60 porque esa migración ya se aplicó: cambiarla
-- altera su checksum y Flyway deja de arrancar en las bases que ya la corrieron.
INSERT INTO elemento_personalizacion (nombre, descripcion, tipo, costo_puntos, activo, logro_requerido_id)
VALUES
    ('Leyenda de la Arena', 'Título reservado para quienes dejan su huella en cada competición.', 'TITULO', 400, TRUE, NULL),
    ('Eclipse', 'Insignia que representa una presencia imposible de ignorar.', 'INSIGNIA', 450, TRUE, NULL);
