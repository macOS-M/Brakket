-- =====================================================================
-- Brakket - RF-19 Control de roles y permisos
-- Agrega jerarquía a roles (catálogo de plataforma), estado de cuenta
-- a usuarios, y el catálogo de permisos con su relación rol_permiso.
-- =====================================================================

-- Jerarquía: 1 = mayor autoridad. Se agrega con UPDATE porque rol ya tiene filas.
ALTER TABLE rol ADD COLUMN nivel INT;
UPDATE rol SET nivel = CASE nombre_rol
                           WHEN 'ADMIN'        THEN 1
                           WHEN 'COMISIONADO'  THEN 2
                           WHEN 'ARBITRO'      THEN 3
                           WHEN 'CAPITAN'      THEN 4
                           WHEN 'JUGADOR'      THEN 5
                           WHEN 'PATROCINADOR' THEN 5
                           ELSE 5
    END;
ALTER TABLE rol ALTER COLUMN nivel SET NOT NULL;

-- Estado de cuenta y de perfil (criterios RF-19)
ALTER TABLE usuario ADD COLUMN bloqueado       BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN perfil_completo BOOLEAN NOT NULL DEFAULT FALSE;

-- Catálogo de permisos
CREATE TABLE permiso (
                         id          BIGSERIAL PRIMARY KEY,
                         codigo      VARCHAR(60) NOT NULL UNIQUE,
                         descripcion VARCHAR(255)
);

CREATE TABLE rol_permiso (
                             rol_id     BIGINT NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
                             permiso_id BIGINT NOT NULL REFERENCES permiso(id) ON DELETE CASCADE,
                             PRIMARY KEY (rol_id, permiso_id)
);

INSERT INTO permiso (codigo, descripcion) VALUES
                                              ('GESTIONAR_ROLES',        'Asignar y revocar roles de usuarios.'),
                                              ('GESTIONAR_USUARIOS',     'Administrar cuentas de usuario (bloquear, editar).'),
                                              ('GESTIONAR_LIGAS',        'Crear y administrar ligas.'),
                                              ('GESTIONAR_TORNEOS',      'Crear y administrar torneos dentro de una liga.'),
                                              ('ARBITRAR_PARTIDAS',      'Registrar resultados y sanciones de partidas.'),
                                              ('RESOLVER_DISPUTAS',      'Atender y resolver disputas de resultados.'),
                                              ('GESTIONAR_EQUIPO',       'Administrar el roster y datos del equipo propio.'),
                                              ('PARTICIPAR_PARTIDAS',    'Participar en partidas programadas.'),
                                              ('VER_ESTADISTICAS',       'Consultar estadísticas segmentadas por juego.'),
                                              ('GESTIONAR_PATROCINIOS',  'Administrar paquetes y contratos de patrocinio de eventos.'),
                                              ('VER_METRICAS_AUDIENCIA', 'Consultar métricas de audiencia y sentimiento del chat.');

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON (
    (r.nombre_rol = 'ADMIN' AND p.codigo IN (
                                             'GESTIONAR_ROLES','GESTIONAR_USUARIOS','GESTIONAR_LIGAS','GESTIONAR_TORNEOS',
                                             'RESOLVER_DISPUTAS','VER_ESTADISTICAS','GESTIONAR_PATROCINIOS','VER_METRICAS_AUDIENCIA'))
        OR (r.nombre_rol = 'COMISIONADO' AND p.codigo IN (
                                                          'GESTIONAR_LIGAS','GESTIONAR_TORNEOS','RESOLVER_DISPUTAS',
                                                          'VER_ESTADISTICAS','VER_METRICAS_AUDIENCIA'))
        OR (r.nombre_rol = 'ARBITRO' AND p.codigo IN (
                                                      'ARBITRAR_PARTIDAS','RESOLVER_DISPUTAS','VER_ESTADISTICAS'))
        OR (r.nombre_rol = 'CAPITAN' AND p.codigo IN (
                                                      'GESTIONAR_EQUIPO','PARTICIPAR_PARTIDAS','VER_ESTADISTICAS'))
        OR (r.nombre_rol = 'JUGADOR' AND p.codigo IN (
                                                      'PARTICIPAR_PARTIDAS','VER_ESTADISTICAS'))
        OR (r.nombre_rol = 'PATROCINADOR' AND p.codigo IN (
                                                           'GESTIONAR_PATROCINIOS','VER_METRICAS_AUDIENCIA'))
    );