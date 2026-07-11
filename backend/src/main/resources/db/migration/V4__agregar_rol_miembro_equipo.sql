-- RF-09: rol del integrante dentro del equipo (CAPITAN, TITULAR, SUPLENTE, COACH)
ALTER TABLE miembro_equipo ADD COLUMN rol VARCHAR(30) NOT NULL DEFAULT 'TITULAR';

-- Sincroniza hacia el nuevo campo el capitan que ya estaba definido en equipo.capitan_id
UPDATE miembro_equipo me
SET rol = 'CAPITAN'
    FROM equipo e
WHERE e.id = me.equipo_id
  AND e.capitan_id = me.usuario_id;

-- Historial de cambios de rol dentro de un equipo (RF-09: "el sistema registra el cambio de rol")
CREATE TABLE equipo_rol_historial (
                                      id             BIGSERIAL PRIMARY KEY,
                                      equipo_id      BIGINT NOT NULL REFERENCES equipo(id) ON DELETE CASCADE,
                                      usuario_id     BIGINT NOT NULL REFERENCES usuario(id),
                                      rol_anterior   VARCHAR(30) NOT NULL,
                                      rol_nuevo      VARCHAR(30) NOT NULL,
                                      fecha          TIMESTAMP NOT NULL DEFAULT now(),
                                      responsable_id BIGINT NOT NULL REFERENCES usuario(id)
);