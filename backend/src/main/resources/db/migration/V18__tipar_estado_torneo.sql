-- Mantiene la base de datos alineada con EstadoTorneo.
ALTER TABLE torneo
    ADD CONSTRAINT ck_torneo_estado
    CHECK (estado IN (
        'BORRADOR',
        'INSCRIPCION_ABIERTA',
        'PROGRAMADO',
        'EN_CURSO',
        'FINALIZADO',
        'CANCELADO'
    ));

CREATE INDEX idx_torneo_estado ON torneo (estado);
