# Decisiones de diseño — Brakket

## DD-01 · Modelo abierto de organizadores (2026-07-22)

**Decisión.** Inspirados en Challenger Mode, abrimos la creación de
competencias: **cualquier usuario autenticado puede crear ligas y
torneos**. Quien crea una entidad queda como su comisionado/organizador
(dueño contextual, igual que el capitán con su equipo). La autorización
queda así:

- **Crear** liga/torneo → cualquier usuario autenticado.
- **Editar / borrar / gestionar** → solo el dueño de la entidad o un ADMIN.
- **Gestión del catálogo de juegos** (alta manual de respaldo, edición,
  perfil competitivo, desactivar) → solo ADMIN. El catálogo se puebla
  automáticamente desde la API de RAWG y cualquier usuario con sesión
  puede importar títulos que falten (los datos vienen de la API, no del
  usuario, así que no hay riesgo de datos basura).

**Desviación de la ERS.** La ERS asume que el rol COMISIONADO lo habilita
un administrador (RF-19) y que solo un comisionado crea ligas y torneos
(RF-22/RF-24). Adoptamos el modelo abierto porque es el estándar de las
plataformas de torneos reales y elimina la fricción de onboarding ("entro
y no puedo hacer nada"). **La habilitación por administrador queda como
configuración futura de la plataforma** (un flag "modo curado" que
restrinja la creación a roles habilitados); el modelo de permisos por rol
(RF-19) sigue vigente para la administración global.

## DD-02 · Torneos comunitarios o de liga (2026-07-22)

Un torneo cuelga **siempre de un juego** y, opcionalmente, de una
temporada de liga (`temporada_id` nullable, migración V22):

- **Comunitario**: sin liga; lo organiza cualquier usuario.
- **De liga**: hospedado en una temporada; solo el comisionado de esa
  liga (o un ADMIN) puede hospedarlo ahí. Se conserva la trazabilidad
  juego → liga → temporada → torneo de la ERS cuando existe.

Visibilidad: campo `publico`. Público → se lista y acepta inscripciones
abiertas; privado → no se lista y solo lo ve su organizador (o ADMIN).

## DD-03 · Perfil competitivo como curaduría opcional (2026-07-22)

RF-21 se reinterpreta: el perfil competitivo de un juego **no es un
requisito** para crear torneos, sino una capa de curaduría del ADMIN.
Sin perfil valen los defaults (1v1…5v5, todos los formatos del
catálogo); con perfil, el wizard y el backend acotan el tamaño de
equipo a la plantilla mínima/máxima definida.

## Deuda técnica registrada

- Invitaciones a torneos privados (hoy: privado = oculto, sin flujo de
  invitación).
- Aprobación de inscripciones por el organizador (hoy: inscripción
  directa hasta llenar cupo).
- Brackets y fixtures (RF-26/RF-27) y reporte de resultados (RF-29+).
- Formatos por puntaje/leaderboard para juegos que no son "A contra B"
  (el catálogo de formatos lo admite como fila futura).
- Flag "modo curado" para restringir la creación a roles habilitados
  (vuelta al modelo estricto de la ERS si el curso lo exige).
- Editar liga con override de ADMIN (hoy el PUT es solo del dueño; el
  ADMIN modera vía eliminación).
- Ciclo de vida del torneo: no hay endpoint que transicione `EstadoTorneo`
  (queda `INSCRIPCION_ABIERTA`; las fechas frenan inscripciones pero el
  estado no avanza). Colateral conocido: un equipo con inscripción
  `CONFIRMADA` no puede disolverse hasta que exista esa gestión.
- Hospedar torneo no valida que `fechaInicio` caiga dentro del rango de
  la temporada elegida.
- Carrera en el cupo de inscripción (count-then-insert sin bloqueo): dos
  capitanes simultáneos en el último cupo pueden excederlo en 1.
- Mensajes del login local revelan si un correo existe y su método de
  acceso (enumeración de cuentas); tampoco hay rate-limit en
  `/api/auth/login`.
- Desactivar juego chequea ligas pero no torneos comunitarios activos;
  eliminar liga arrastra en cascada torneos con equipos inscritos.
