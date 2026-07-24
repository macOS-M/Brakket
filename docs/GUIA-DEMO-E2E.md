# Guía E2E de demo — Primera iteración de Brakket

> Guía para presentar la app completa **tal como funciona hoy**. Escrita para que
> cualquiera del equipo la siga sin haber programado el módulo que muestra.
> Regla de oro: **nada se registra ni se carga a mano en vivo** — todo viene del
> seed; solo 3 flujos se ejecutan en vivo porque vale la pena verlos ocurrir.

**Duración total estimada: ~25 min de demo + 5 de cierre.**

---

## 1. Inventario real de módulos (qué se demuestra y qué no)

Verificado contra el código en la rama de esta iteración (no contra el ERS).

| Módulo | Estado | En la demo |
|---|---|---|
| Landing institucional y de producto | ✅ Funcional | Se muestra |
| Registro / login local + Google | ✅ Funcional | Login en vivo (registro NO: precargado) |
| Dashboard `/inicio` | ✅ Funcional | Se muestra (datos reales del seed) |
| Juegos (catálogo, hub, importar RAWG) | ✅ Funcional | Se muestra |
| Ligas y temporadas | ✅ Funcional | Se muestra (precargadas) |
| Torneos (5 formatos, bracket, resultados) | ✅ Funcional | Torneo A precargado + **gran final EN VIVO** |
| Equipos (crear, invitar, roles, perfil público, stats) | ✅ Funcional | Perfil + **aceptar invitación EN VIVO** |
| Transferencias | ✅ Funcional | Opcional en vivo |
| Historial de jugador (`/players/:id/historial`) | ✅ Funcional | Se muestra |
| Perfil propio (`/profile`) | ✅ Funcional | Se muestra rápido |
| Transmisiones (`/transmisiones`, RF-35) | ✅ Funcional | Se muestra; EN VIVO si el canal transmite |
| Canal de Twitch + métricas (`/twitch`, RF-34/36) | ✅ Funcional | Se muestra (ADMIN) |
| Patrocinios (`/sponsorships`, RF-41) | ✅ Funcional | Se muestra (precargados) |
| Administración de roles (`/admin`, RF-19) | ✅ Funcional | Se muestra (ADMIN) |
| Disputas | 🔜 Próximamente (placeholder) | **NO demostrable** (la resolución de resultados en torneos SÍ existe) |
| Estadísticas (página propia) | 🔜 Próximamente | **NO demostrable** (las stats del perfil de equipo SÍ) |
| Progresión | 🔜 Próximamente | NO demostrable |
| Notificaciones (página) | 🔜 Próximamente | NO demostrable (los "pendientes" del dashboard SÍ) |
| Analítica / Panel comercial del patrocinador | 🔜 Próximamente | NO demostrable |
| Chat de Twitch, sentimiento IA, termómetro (RF-38/39/40) | ❌ No implementado | NO demostrable |
| Consulta de métricas por período/rango (RF-37) | ❌ No implementado | NO demostrable (los indicadores básicos de RF-36 SÍ) |

Otros límites honestos: el rol ÁRBITRO existe pero no participa del flujo de
resultados (reporta capitán, confirma rival, resuelve organizador/admin); no hay
perfil público de jugador más allá del historial de equipos.

---

## 2. Datos semilla

### 2.1 Cuentas — opciones evaluadas y elección

Bracket sin byes = potencia de 2. Cuentas de staff fijas: 1 admin + 1 organizadora.

| Opción | Cuentas de jugadores | Total cuentas |
|---|---|---|
| **4 equipos × 1 persona (ELEGIDA)** | 4 | **6** |
| 8 equipos × 1 persona | 8 | 10 |
| 4 equipos × 2 personas | 8 | 10 |

Se eligió 4×1: brackets de 3 (eliminación) y 6 (doble eliminación) partidas,
suficiente para contar la historia sin inflar cuentas ni alargar la demo. Los
**mismos 4 equipos juegan ambos torneos**.

### 2.2 Credenciales de demo (contraseña única: `Demo2026!`)

⚠️ Credenciales obvias y públicas a propósito. Solo existen en entornos dev.

| Cuenta | Correo | Rol / función |
|---|---|---|
| Adriana Admin | `admin.demo@brakket.gg` | **ADMIN** — /admin, /twitch, patrocinios |
| Olivia Organizadora | `orga.demo@brakket.gg` | Comisionada de la liga, organizadora de los torneos |
| Ana Fénix | `cap1.demo@brakket.gg` | Capitana de **Fenix Demo** |
| Bruno Lobo | `cap2.demo@brakket.gg` | Capitán de **Lobos Demo** |
| Carla Cuervo | `cap3.demo@brakket.gg` | Capitana de **Cuervos Demo** |
| Diego Titán | `cap4.demo@brakket.gg` | Capitán de **Titanes Demo** |

### 2.3 Qué deja montado el seed

- **Jerarquía**: Liga Demo Brakket (Rocket League) → Temporada Demo 2026 (ACTIVA) → 2 torneos 1v1.
- **Torneo A — "Copa Relampago (Demo)"**: eliminación directa, 4 equipos, **FINALIZADO**
  con las 3 partidas cerradas, campeón coronado, y todo lo derivado (bracket
  completo, estadísticas del perfil de equipo, historial).
- **Torneo B — "Copa Doble Orbita (Demo)"**: doble eliminación, 4 equipos, **EN
  CURSO** con llave superior e inferior resueltas y **solo la GRAN FINAL
  pendiente** — el momento "mirá cómo avanza el bracket" de la presentación.
- 1 invitación de equipo **pendiente** (Fenix Demo → Olivia) para que el
  dashboard muestre acciones y se pueda aceptar en vivo.
- 2 patrocinadores demo (Cafe Volcan, Nebula Energy).
- Best-effort: canal oficial de Twitch validado + transmisión asociada al
  Torneo B (si el `.env` tiene credenciales de Twitch).

### 2.4 Cómo correrlo (y resetear para ensayar)

```bash
# Requisitos: docker compose up -d (backend en localhost:8080) y Node 18+.
node scripts/seed-demo.mjs               # reset + seed (idempotente: se puede correr N veces)
node scripts/seed-demo.mjs --solo-reset  # solo borrar los datos demo
```

- **Guardas**: aborta si `SPRING_PROFILES_ACTIVE=prod` o si el backend no responde.
- **Solo borra datos demo** (nombres `(Demo)` y correos `*.demo@brakket.gg`); jamás toca datos reales.
- Los IDs cambian en cada corrida: en la demo **navegá siempre por nombre**, nunca por URL con ID memorizado.
- Crea todo **vía API real** (mismas reglas de negocio que la UI), salvo el grant de ADMIN, que es un INSERT dev-only.

### 2.5 Preparación previa (20 min antes, NO en escena)

1. `docker compose up -d` y verificar `http://localhost:8080/actuator/health` = UP.
2. `node scripts/seed-demo.mjs`.
3. `ng serve` en `frontend/` — **abrí la app y verificá que /transmisiones carga**;
   si el bundle quedó viejo, reiniciá `ng serve` (falla conocida del watch).
4. Preparar **3 perfiles/ventanas de navegador** ya logueados:
   - Ventana 1 (principal): `orga.demo@brakket.gg`
   - Ventana 2: `cap3.demo@brakket.gg` (Cuervos — reporta la gran final)
   - Ventana 3: `cap4.demo@brakket.gg` (Titanes — confirma) — puede ser incógnito
   - Tener a mano las credenciales de `admin.demo@brakket.gg` (se usa la Ventana 1 al final, cerrando sesión).
5. Opcional (módulo Twitch al máximo): dejar OBS o la app de Twitch lista para
   poner en vivo el canal oficial en la sección 9.

---

## 3. Recorrido paso a paso

Leyenda: 🖥️ = ya está cargado, solo se muestra · 🔴 = se ejecuta EN VIVO.

### Sección 1 — Portada y acceso (2 min) — Ventana 1

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 1.1 🖥️ | (nadie) | Abrir `http://localhost:4200/` | Landing institucional; botón "Ver plataforma" |
| 1.2 🖥️ | (nadie) | `/producto` | Landing del producto |
| 1.3 🔴 | — | Login con `orga.demo@brakket.gg` / `Demo2026!` | Entra al dashboard. Mencionar que también hay login con Google (los correos del equipo entran como ADMIN) |

### Sección 2 — Dashboard (2 min) — Ventana 1 (Olivia)

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 2.1 🖥️ | Olivia | Recorrer `/inicio` | Carrusel de juegos, rail de **próximos torneos** (Copa Doble Orbita EN_CURSO), "Tus competencias" (organiza los 2 torneos) y **Pendientes: 1 invitación de Fenix Demo** |

### Sección 3 — Juegos, liga y temporada (3 min) — Ventana 1 (Olivia)

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 3.1 🖥️ | Olivia | `/games` → abrir Rocket League | Catálogo (5 juegos) y hub del juego. Mencionar: importar desde RAWG e (ADMIN) crear/editar |
| 3.2 🖥️ | Olivia | `/leagues` → **Liga Demo Brakket** | Detalle de la liga; Olivia es la comisionada |
| 3.3 🖥️ | Olivia | Tab/sección de temporadas | **Temporada Demo 2026** ACTIVA con cupo y formato; los 2 torneos cuelgan de ella. Narrar la jerarquía liga → temporada → torneo |

### Sección 4 — Equipos + invitación EN VIVO (3 min) — Ventana 1 (Olivia)

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 4.1 🖥️ | Olivia | `/teams` → buscar "Fenix" → perfil público | Tarjeta del club, tabs Resumen / Miembros / **Estadísticas** (victorias/derrotas/winrate REALES del Torneo A) |
| 4.2 🔴 | Olivia | Dashboard o `/teams/invitaciones` → **aceptar** la invitación de Fenix Demo | Olivia queda SUPLENTE; la plantilla de Fenix Demo ahora muestra 2 miembros |
| 4.3 🖥️ | Olivia | Desde el perfil del equipo → historial / miembros → historial de jugador | Historial de equipos del jugador (RF-15/16) |

### Sección 5 — Torneo A: el ciclo completo ya jugado (3 min) — Ventana 1 (Olivia)

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 5.1 🖥️ | Olivia | `/tournaments` → **Copa Relampago (Demo)** | Estado FINALIZADO, 4 equipos, formato eliminación directa |
| 5.2 🖥️ | Olivia | Abrir el bracket | Llave completa con marcadores, camino del campeón resaltado y **campeón coronado** |
| 5.3 🖥️ | Olivia | Volver al perfil de un equipo participante | Las estadísticas del tab reflejan estas partidas — nada es simulado |

### Sección 6 — Torneo B: la GRAN FINAL en vivo (5 min) — Ventanas 2 y 3 ⭐

El momento estrella. Narrar: "esto que sigue no está pregrabado".

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 6.1 🖥️ | Olivia (V1) | `/tournaments` → **Copa Doble Orbita (Demo)** → bracket | **Doble eliminación**: llave superior, llave inferior y la **gran final dorada PENDIENTE** (Cuervos vs Titanes*) |
| 6.2 🖥️ | Carla (V2) | Abrir la misma partida como capitana | Ve el **lobby con clave** (solo capitanes/organizador la ven) |
| 6.3 🔴 | Carla (V2) | **Reportar resultado** de la gran final (p. ej. 3–2) | Partida pasa a REPORTADA |
| 6.4 🔴 | Diego (V3) | **Confirmar** el resultado | La partida se cierra, el bracket avanza, **campeón coronado y torneo FINALIZADO** (confetti) |
| 6.5 🖥️ | Olivia (V1) | Refrescar el bracket | Todo el torneo cerrado; mencionar que rechazar → disputa → la resuelve el organizador |

\* Los finalistas pueden variar entre corridas del seed — leerlos del bracket, no de esta guía.

### Sección 7 — Transferencias (2 min, OPCIONAL si hay tiempo) — Ventanas 2/1

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 7.1 🔴 | Bruno (o cap2 en V3) | `/transfers` → crear solicitud por **Olivia** (ahora suplente de Fenix) hacia Lobos Demo | Solicitud pendiente creada |
| 7.2 🔴 | La otra parte | Responder la solicitud | Flujo completo de RF-13/14. **Fallback**: si algo falla, mostrar solo el listado y narrar |

### Sección 8 — Transmisiones y Twitch (4 min) — Ventana 1

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 8.1 🖥️ | (público) | `/transmisiones` (sirve sin sesión) | Vitrina estilo Twitch: hero con el canal oficial, estado real consultado a Helix. Offline = tarjeta digna con placeholder "Próximamente"; si el canal está EN VIVO: player, badge, espectadores en ~1 min |
| 8.2 🖥️ | **Admin** (login en V1) | `/twitch` ("Canal de Twitch" en el menú) | Canal validado (RF-34), transmisión asociada al Torneo B |
| 8.3 🖥️ | Admin | Bloque **Métricas de audiencia (RF-36)** | Muestras/pico/promedio/duración. Si el canal no transmitió: "Todavía no hay muestras" — narrar que el muestreo corre cada 60 s automáticamente |

### Sección 9 — Patrocinios y administración (3 min) — Ventana 1 (Admin)

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 9.1 🖥️ | Admin | `/sponsorships` | Cafe Volcan y Nebula Energy precargados; formulario de crear/editar (crear uno en vivo es opcional y rápido) |
| 9.2 🖥️ | Admin | `/admin` | Catálogo de roles y permisos (RF-19); buscar usuario y asignar/revocar rol (si se hace en vivo: dar ARBITRO a Diego y revocarlo) |
| 9.3 🖥️ | Admin | `/profile` (opcional) | Edición de perfil propio (RF-18) |

### Sección 10 — Cierre (2 min)

Mostrar el menú con los badges "Pronto" y ser explícitos: disputas como módulo,
estadísticas globales, progresión, notificaciones, panel comercial del
patrocinador, chat de Twitch + análisis de sentimiento (RF-38/39/40) y la
consulta de métricas por período (RF-37) son la **siguiente iteración**. Lo
mostrado hoy es funcional de punta a punta sobre datos reales.

### Tiempos

| Sección | Min | Acumulado |
|---|---|---|
| 1. Portada y acceso | 2 | 2 |
| 2. Dashboard | 2 | 4 |
| 3. Juegos, liga, temporada | 3 | 7 |
| 4. Equipos + invitación | 3 | 10 |
| 5. Torneo A finalizado | 3 | 13 |
| 6. **Gran final en vivo** | 5 | 18 |
| 7. Transferencias (opcional) | 2 | 20 |
| 8. Transmisiones + Twitch | 4 | 24 |
| 9. Patrocinios + admin | 3 | 27 |
| 10. Cierre | 2 | **29** |

Si hay que recortar: sacrificar 7 (transferencias) y 9.3 (perfil).

---

## 4. Puntos frágiles y qué hacer si fallan

| Riesgo | Síntoma | Mitigación / plan B |
|---|---|---|
| `ng serve` sirve bundle viejo | Rutas nuevas redirigen al landing | Reiniciar `ng serve` ANTES de presentar (paso 2.5.3) |
| Backend sin credenciales Twitch | El backend **no arranca** (fail-fast) | Poner `TWITCH_CLIENT_ID/SECRET` en `.env`, o `TWITCH_REQUIRED=false` y recrear el contenedor |
| `.env` editado sin recrear | Cambios no aplican | `docker compose up -d --force-recreate backend` (restart NO alcanza) |
| Twitch/Helix caído en escena | `/transmisiones` muestra aviso ámbar "estado desconocido" | Narrarlo como feature: degradación ante fallos (RNF-15). No es un error |
| Confusión de ventanas en la gran final | 403 "Solo un capitán…" al reportar/confirmar | Verificar la cuenta en el header. **Plan B**: Olivia (organizadora) fija el resultado con "Resolver" y se narra la disputa |
| Finalistas distintos a los de la guía | La gran final no es Cuervos vs Titanes | Normal entre corridas del seed: leer los nombres del bracket |
| Se ensució el estado ensayando | Datos demo inconsistentes | `node scripts/seed-demo.mjs` re-crea todo desde cero (~30 s). Los IDs cambian: navegar por nombre |
| Ventana minimizada/tapada | Animaciones (confetti, avance) no se ven | Mantener la ventana del navegador visible y al frente |
| Internet caído | Login Google, RAWG y Twitch fallan | Todo lo demás es local: usar solo cuentas demo y narrar las integraciones |

---

## 5. Reparto sugerido (5 presentadores)

1. **Apertura** (secciones 1–2): visión del producto + dashboard.
2. **Competencia** (3–5): liga, temporada, equipos, torneo finalizado.
3. **El momento en vivo** (6–7): gran final + transferencia. Requiere ensayo con las 3 ventanas.
4. **Streaming** (8): transmisiones + métricas (RF-34/35/36).
5. **Plataforma** (9–10): patrocinios, roles, cierre y roadmap.

Ensayo recomendado: correr el seed, cronometrar una pasada completa, resetear y
repetir. El seed existe exactamente para eso.
