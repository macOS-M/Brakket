-- RF-36: captura periódica de métricas de audiencia.
--
-- El muestreo real ancla en transmision_twitch (RF-35); la cuenta por equipo
-- (cuenta_twitch, modelo previo de EPIC-10) deja de ser obligatoria, pero toda
-- muestra debe colgar de al menos una de las dos fuentes.
ALTER TABLE metrica_audiencia ALTER COLUMN cuenta_twitch_id DROP NOT NULL;
ALTER TABLE metrica_audiencia
    ADD CONSTRAINT ck_metrica_aud_ancla
        CHECK (cuenta_twitch_id IS NOT NULL OR transmision_twitch_id IS NOT NULL);

-- El ERS exige distinguir datos reales de simulados (contexto académico).
-- La tabla está vacía hasta hoy (nadie escribía en ella), así que el DEFAULT
-- no re-etiqueta nada preexistente.
ALTER TABLE metrica_audiencia
    ADD COLUMN origen VARCHAR(20) NOT NULL DEFAULT 'REAL'
        CONSTRAINT ck_metrica_aud_origen CHECK (origen IN ('REAL', 'SIMULADO'));
