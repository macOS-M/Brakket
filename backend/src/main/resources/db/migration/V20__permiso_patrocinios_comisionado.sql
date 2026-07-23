-- Alinea el seed de permisos con la ERS: RF-42 y RF-43 dicen "como
-- administrador o comisionado", y ACT-02 define al comisionado como quien
-- gestiona los acuerdos comerciales de su liga. El seed original (V8) le
-- daba GESTIONAR_PATROCINIOS solo a ADMIN.
--
-- ⚠️ ORDEN DE MERGE: esta migración es V20 porque V19 está reservada por
-- el PR de RF-10 (expulsar integrante). Ese PR debe mergearse ANTES que
-- este; con out-of-order desactivado, Flyway rechaza una V19 que llegue
-- después de aplicada la V20.

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
         JOIN permiso p ON p.codigo = 'GESTIONAR_PATROCINIOS'
WHERE r.nombre_rol = 'COMISIONADO'
  AND NOT EXISTS (SELECT 1
                  FROM rol_permiso rp
                  WHERE rp.rol_id = r.id
                    AND rp.permiso_id = p.id);
