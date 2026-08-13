-- RF-38: contenido de los mensajes del chat con su marca temporal.
--
-- Hasta ahora solo se persistían los agregados por ventana (metrica_chat), y
-- cuatro criterios del requerimiento piden el detalle: "registra hora y
-- contenido del mensaje", "cada mensaje capturado queda asociado a hora,
-- transmisión y contenido disponible", "asocia cada dato capturado con la
-- transmisión, torneo y marca temporal" y "permite consultar los datos
-- capturados desde los módulos de reportes".
--
-- NO hay columna de autor, y es deliberado: el nick se usa en memoria solo para
-- contar usuarios distintos y nunca llega acá. Así la regla de privacidad que el
-- ERS lista como dependencia queda garantizada por el esquema y no por una
-- convención que alguien pueda romper después sin darse cuenta.
CREATE TABLE mensaje_chat (
    id                    BIGSERIAL PRIMARY KEY,
    transmision_twitch_id BIGINT       NOT NULL REFERENCES transmision_twitch (id) ON DELETE CASCADE,
    -- Twitch limita los mensajes a 500 caracteres.
    texto                 VARCHAR(500) NOT NULL,
    fecha_hora            TIMESTAMP    NOT NULL
);

-- Consulta por franja horaria dentro de una transmisión, que es como el
-- asistente arma su contexto.
CREATE INDEX idx_mensaje_chat_transmision ON mensaje_chat (transmision_twitch_id, fecha_hora);

-- Búsqueda por tema. Se indexa con la configuración 'spanish' porque descarta
-- artículos y preposiciones y lematiza; el chat mezcla idiomas, pero el
-- castellano domina y para inglés el peor caso es no lematizar.
CREATE INDEX idx_mensaje_chat_texto ON mensaje_chat USING GIN (to_tsvector('spanish', texto));
