-- Ficha enriquecida del juego desde RAWG (se llena al importar/sembrar;
-- después todo se sirve desde la BD sin gastar cuota de API).
ALTER TABLE juego ALTER COLUMN descripcion TYPE TEXT;
ALTER TABLE juego ADD COLUMN rawg_slug VARCHAR(200);
ALTER TABLE juego ADD COLUMN fecha_lanzamiento DATE;
ALTER TABLE juego ADD COLUMN rating DOUBLE PRECISION;
ALTER TABLE juego ADD COLUMN metacritic INT;
ALTER TABLE juego ADD COLUMN plataformas VARCHAR(300);
ALTER TABLE juego ADD COLUMN etiquetas VARCHAR(500);
ALTER TABLE juego ADD COLUMN sitio_web VARCHAR(300);
-- URLs de capturas (JSON array) para la galería del hub.
ALTER TABLE juego ADD COLUMN capturas TEXT;
