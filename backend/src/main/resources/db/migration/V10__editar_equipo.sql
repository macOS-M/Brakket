-- =====================================================================
-- Brakket - RF-02 Editar equipo
-- Agrega estado del equipo (ciclo de vida), estado de privacidad, y
-- control de concurrencia optimista para ediciones simultáneas.
-- =====================================================================

-- Ciclo de vida del equipo: ACTIVO (normal), BLOQUEADO (disputa/revisión
-- administrativa activa), DISUELTO (RF-03). La columna la introduce RF-03
-- (V7__disolver_equipo, que se integra antes); el IF NOT EXISTS queda solo
-- como red de seguridad para correr esta rama de forma aislada.
ALTER TABLE equipo ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

-- Visibilidad del perfil público del equipo. Misma convención que
-- usuario.visibilidad_perfil (PUBLIC/PRIVATE).
ALTER TABLE equipo ADD COLUMN estado_privacidad VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

-- Control de concurrencia optimista (criterio RF-02: "si dos usuarios
-- intentan modificar información relacionada al mismo tiempo, el sistema
-- conserva la versión válida más reciente"). JPA @Version la incrementa
-- sola en cada UPDATE y rechaza el guardado si la fila cambió entre medio.
ALTER TABLE equipo ADD COLUMN version BIGINT NOT NULL DEFAULT 0;