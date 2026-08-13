-- Traduce al español las etiquetas de los juegos ya importados.
--
-- Los temas de IGDB llegaban en inglés y se guardaban tal cual, así que la
-- ficha mostraba "Action · Fantasy · Warfare" en una interfaz que por lo demás
-- está entera en español. A partir de ahora IgdbGameSearchServiceImpl los
-- traduce al importar; esta migración arregla las filas que ya existen, porque
-- el CatalogoSeeder solo completa fichas incompletas y no vuelve a tocar un
-- juego que ya tiene rating.
--
-- El vocabulario de temas de IGDB es cerrado y ninguno es subcadena de otro,
-- así que reemplazar por nombre completo es seguro. Van como sentencias
-- sueltas y no como REPLACE anidados: encadenar veinte llamadas en una sola
-- expresión se desbalancea con solo agregar un tema.

-- Primero el único tema que trae comas dentro del nombre: al guardarse en una
-- lista separada por comas rompía el split de la ficha, que lo mostraba como
-- tres etiquetas sin sentido.
UPDATE juego SET etiquetas = REPLACE(etiquetas, '4X (explore, expand, exploit, and exterminate)', '4X');

UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Science fiction', 'Ciencia ficción');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Non-fiction', 'No ficción');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Open world', 'Mundo abierto');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Historical', 'Histórico');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Educational', 'Educativo');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Survival', 'Supervivencia');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Business', 'Negocios');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Mystery', 'Misterio');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Thriller', 'Suspenso');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Fantasy', 'Fantasía');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Warfare', 'Bélico');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Comedy', 'Comedia');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Erotic', 'Erótico');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Action', 'Acción');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Horror', 'Terror');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Stealth', 'Sigilo');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Party', 'Fiesta');
UPDATE juego SET etiquetas = REPLACE(etiquetas, 'Kids', 'Infantil');
