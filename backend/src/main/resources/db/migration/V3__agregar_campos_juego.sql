-- Agrega campos de imagen y descripcion al catalogo de juegos (RF-20)
ALTER TABLE juego ADD COLUMN imagen_url VARCHAR(500);
ALTER TABLE juego ADD COLUMN descripcion VARCHAR(1000);