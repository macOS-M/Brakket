-- Identificador del video de YouTube provisto por IGDB.
ALTER TABLE juego ADD COLUMN trailer_id VARCHAR(32);
ALTER TABLE juego ADD COLUMN trailer_consultado BOOLEAN NOT NULL DEFAULT FALSE;
