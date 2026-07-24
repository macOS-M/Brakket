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

## DD-04 · Login local además de Google (2026-07-22)

Registro e inicio de sesión con correo y contraseña (BCrypt, V24) que
emite el mismo JWT que el flujo de Google. Motivo: la app de OAuth está
en modo testing y solo admite usuarios allowlisted; el login local
desbloquea demos y usuarios reales. El bootstrap de ADMIN por correo del
equipo aplica **solo** vía Google (correo verificado); registrarse
localmente con un correo del equipo no otorga ningún rol especial. Si el
correo ya existía como cuenta local, "Continuar con Google" le vincula el
`googleId` en vez de duplicarla.

## DD-05 · Torneo en vivo sin API del juego (2026-07-23)

**Contexto.** Los juegos (Rocket League incluido) no exponen API pública
para inyectar torneos ni leer resultados. Como las plataformas reales,
Brakket es la **capa organizativa por fuera del juego** y el puente son
las personas:

- **Iniciar torneo** (organizador o ADMIN, ≥2 inscritos): cierra la
  inscripción, genera la estructura del **formato elegido** (RF-26/27) y
  pone el torneo `EN_CURSO`. Los cinco formatos del catálogo tienen
  motor real (V31): eliminación directa (potencia de 2 con byes a los
  primeros inscritos), doble eliminación (llave inferior + gran final
  única, sin bracket reset), round robin (método del círculo; campeón
  por victorias → diferencia → puntos a favor), suizo (⌈log2 n⌉ rondas
  generadas al cerrarse la anterior, emparejando por marcas y evitando
  revanchas; los byes rotan) y fase de grupos + eliminación (grupos de
  ~4 por reparto alterno, avanzan los dos primeros a una llave cruzada
  1° vs 2° del grupo hermano; exige ≥4 inscritos). En ningún formato
  hay empates: el marcador debe definir un ganador.
- **Lobby por partida**: cada cruce recibe nombre y clave autogenerados
  (`BRAKKET-T7-R1M2` + clave dictable). Los capitanes crean esa partida
  privada dentro del juego; Brakket no la crea ni la verifica.
- **Gamertag en la inscripción** (`usuario_en_juego`, V26): identidad
  declarada dentro del juego, para saber a quién invitar a la lobby y
  quién jugó. Sin API no hay verificación técnica: es dato declarado.
- **Ajustes de partida** (pares clave/valor, V26): el organizador define
  el "Game settings" del torneo (modo, arena, duración…). Brakket lo
  publica como contrato; ante una disputa, esa configuración manda.
- **Resultados (RF-29 mínimo)**: un capitán reporta, el rival confirma o
  rechaza; en disputa (o con un rival ausente) resuelve el organizador o
  un ADMIN. El resultado confirmado avanza la llave; la final corona al
  campeón y el torneo queda `FINALIZADO` (los equipos vuelven a poder
  disolverse).

## DD-06 · Ajustes personales del perfil (2026-07-23)

RF-18 se amplía: el perfil separa la **identidad pública** (nombre visible,
avatar, biografía, redes, juegos) de los **ajustes personales** privados
(nombre legal, fecha de nacimiento, teléfono, dirección, ciudad, país,
código postal y zona horaria, migración V32). Solo el dueño de la cuenta
los ve: viajan en `GET/PUT /api/me` y no en el perfil público.

Todos se guardan con el mismo `PUT /api/me` que el resto del perfil: un
solo formulario, un solo botón de guardar. Dos reglas en el backend:

- **Teléfono normalizado**: se guarda solo con dígitos, conservando el
  `+` del prefijo internacional, y se exige entre 8 y 15 dígitos. Así
  "+506 8888-7777" y "+50688887777" quedan idénticos en la base.
- **Edad mínima de 13 años**, validada en el backend
  (`BusinessException`) y reflejada en el `max` del datepicker.

**Sin verificación de teléfono.** Se evaluó un flujo de código por SMS y
se descartó: no hay proveedor contratado, y un "verificado" que en
realidad no verifica nada es peor que no tenerlo. Si más adelante se
necesita (premios, avisos urgentes), entra como columna
`telefono_verificado` más su flujo propio, sin tocar lo ya guardado.

## Deuda técnica registrada

- Teléfono sin verificar (ver DD-06): no hay proveedor de SMS ni columna
  de verificación.
- Los ajustes personales no tienen aún flujo de exportación/borrado de
  datos personales a pedido del usuario.

- Invitaciones a torneos privados (hoy: privado = oculto, sin flujo de
  invitación).
- Aprobación de inscripciones por el organizador (hoy: inscripción
  directa hasta llenar cupo).
- La gran final de la doble eliminación es única (sin bracket reset: el
  invicto no tiene ventaja de "hay que ganarle dos veces").
- La lobby (nombre y clave) es visible para cualquiera que vea el
  torneo; restringirla a los capitanes participantes queda pendiente.
- Disputas formales con evidencia adjunta (RF-30+): hoy el rechazo deja
  la partida EN_DISPUTA y el organizador la resuelve sin evidencias.
- Formatos por puntaje/leaderboard para juegos que no son "A contra B"
  (el catálogo de formatos lo admite como fila futura).
- Flag "modo curado" para restringir la creación a roles habilitados
  (vuelta al modelo estricto de la ERS si el curso lo exige).
- Editar liga con override de ADMIN (hoy el PUT es solo del dueño; el
  ADMIN modera vía eliminación).
- Cancelar torneo (transición a `CANCELADO` liberando inscripciones y
  partidas); iniciar/finalizar ya existen (DD-05).
- Hospedar torneo no valida que `fechaInicio` caiga dentro del rango de
  la temporada elegida.
- Carrera en el cupo de inscripción (count-then-insert sin bloqueo): dos
  capitanes simultáneos en el último cupo pueden excederlo en 1.
- Mensajes del login local revelan si un correo existe y su método de
  acceso (enumeración de cuentas); tampoco hay rate-limit en
  `/api/auth/login`.
- Desactivar juego chequea ligas pero no torneos comunitarios activos;
  eliminar liga arrastra en cascada torneos con equipos inscritos.
