-- V60__rf50_reportes.sql
-- RF-50: Exportar reportes y métricas.
-- Permiso EXPORTAR_REPORTES para los 3 roles que la ERS nombra
-- textualmente (administrador, comisionado, patrocinador autorizado),
-- y tabla de auditoría de reportes generados.

INSERT INTO permiso (codigo, descripcion) VALUES
    ('EXPORTAR_REPORTES', 'Generar y exportar reportes de competencias, audiencia, patrocinio y estadísticas.');

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
         JOIN permiso p ON p.codigo = 'EXPORTAR_REPORTES'
WHERE r.nombre_rol IN ('ADMIN', 'COMISIONADO', 'PATROCINADOR')
  AND NOT EXISTS (
    SELECT 1 FROM rol_permiso rp
    WHERE rp.rol_id = r.id AND rp.permiso_id = p.id
);

CREATE TABLE reporte_generado (
                                  id               BIGSERIAL PRIMARY KEY,
                                  tipo             VARCHAR(30) NOT NULL
                                      CHECK (tipo IN ('COMPETENCIA', 'AUDIENCIA', 'PATROCINIO', 'ESTADISTICA')),
                                  usuario_id       BIGINT NOT NULL REFERENCES usuario(id),
                                  filtros          TEXT,
                                  fecha_generacion TIMESTAMP NOT NULL DEFAULT now()
);

COMMENT ON COLUMN reporte_generado.filtros IS
    'Texto legible de los filtros usados (torneo, período, patrocinador), para auditoría. No estructurado a propósito: solo se usa para mostrar/consultar, no para volver a ejecutar la consulta.';