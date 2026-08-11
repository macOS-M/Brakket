-- RF-34: el indice unico sobre twitch_stream_id era global, asi que un id de
-- directo quedaba tomado para siempre: al finalizar una transmision y querer
-- volver a asociar el mismo directo (Twitch reusa el mismo stream id mientras
-- no se corte la emision) la insercion chocaba contra el indice.
--
-- Lo que hay que impedir es que dos transmisiones ABIERTAS sigan el mismo
-- directo. Una finalizada ya no compite por el muestreo, asi que no debe
-- bloquear.

DROP INDEX IF EXISTS uq_transmision_twitch_stream;

CREATE UNIQUE INDEX uq_transmision_twitch_stream_abierta
    ON transmision_twitch (twitch_stream_id)
    WHERE twitch_stream_id IS NOT NULL AND finalizada_en IS NULL;
