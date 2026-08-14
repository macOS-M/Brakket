-- Rediseño de patrocinios: se retira el concepto de "nivel" (ORO/PLATA/BRONCE)
-- como determinante de exclusividad de espacio. El modelo pasa a ser un solo
-- patrocinio ACTIVO por competencia a la vez (como Ligue 1 + Uber Eats: un
-- patrocinador principal de liga, y torneos que pueden tener uno propio o
-- heredar el de su liga), no varios niveles compitiendo por el mismo espacio.
--
-- Se elimina la columna por completo (no solo nullable): no hay datos de
-- producción que preservar en este proyecto, y un campo fantasma sin uso
-- real es peor que no tenerlo.
--
-- Tampoco se borra `temporada_id` de `patrocinio`: el alcance TEMPORADA se
-- retira del flujo de creación (las temporadas no tienen pantalla propia,
-- viven dentro de la liga), pero la columna se deja intacta por si ya
-- existen filas de prueba con ese alcance.

ALTER TABLE patrocinio DROP COLUMN nivel;

-- Ubicaciones de espacio publicitario: de 5 a 3. Se retiran DASHBOARD_CARD y
-- CALENDARIO_FRANJA porque son pantallas que agregan contenido de MUCHAS
-- ligas/torneos a la vez — no hay un "dueño" natural del espacio ahí.
DELETE FROM espacio_publicitario WHERE ubicacion IN ('DASHBOARD_CARD', 'CALENDARIO_FRANJA');

ALTER TABLE espacio_publicitario DROP CONSTRAINT ck_espacio_publicitario_ubicacion;

ALTER TABLE espacio_publicitario ADD CONSTRAINT ck_espacio_publicitario_ubicacion
    CHECK (ubicacion IN ('TRANSMISION_INFERIOR', 'TORNEO_CABECERA', 'LIGA_CABECERA'));