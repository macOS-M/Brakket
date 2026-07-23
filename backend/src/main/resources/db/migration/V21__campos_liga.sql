-- RF-22: la liga guarda descripción, reglas y una foto propia.
-- La ERS pide crear ligas "con reglas, descripción y responsables"; hasta
-- ahora solo existían nombre, juego y comisionado. La foto es opcional:
-- cuando falta, el frontend usa el arte del juego como portada.

ALTER TABLE liga
    ADD COLUMN descripcion VARCHAR(1000),
    ADD COLUMN reglas      VARCHAR(4000),
    ADD COLUMN foto_url    VARCHAR(500);
