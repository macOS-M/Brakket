# Guía E2E de demo — Segunda iteración de Brakket

> Script para presentar la app **tal como funciona hoy**, escrito para que
> cualquiera del equipo lo siga sin haber programado el módulo que muestra.
> **Regla de oro:** nada se registra ni se carga a mano en vivo salvo los pasos
> marcados 🔴 — todo lo demás viene del seed.
>
> **Todos deben conocer este flujo completo.** El profesor advirtió que la
> inseguridad al mostrar la app se castiga fuerte, y que en medio de la
> presentación puede pedirle a cualquiera del equipo que tome el control.

**Duración estimada: ~30 min de demo + 5 de roadmap/known issues + preguntas.**

---

## 0. Presentación remota por túnel (OBLIGATORIO esta iteración)

El profesor prueba la app **desde su computadora**, por un túnel. Se expone el
backend y el frontend, y se le pasa **la URL del frontend**.

### 0.1 Quién hostea

**Una sola persona** corre todo (base + backend + frontend + túnel) y comparte
**una URL**. Todos los demás —el equipo y el profesor— abren esa misma URL en su
navegador. Nadie se conecta a la computadora del host; solo abren la página.

> **Plan B por si al host se le va la luz/internet:** un segundo compañero deja
> su host **completo y probado** de antemano (mismo `pull`, mismo seed, su propio
> túnel). Si el primero cae, levanta su túnel y se pasa **su** URL. Los datos se
> ven iguales porque ambos corren el mismo seed.

### 0.2 Herramienta: Port Forwarding de VS Code

Se usa el **Port Forwarding nativo de VS Code** (dev tunnels de Microsoft),
no ngrok. Requiere iniciar sesión una vez con GitHub.

1. Backend y frontend corriendo en local (8080 y 4200), en la rama con los
   cambios de túnel mergeados a `develop`.
2. Panel **Ports** de VS Code → **Forward a Port** → `8080`. Repetir con `4200`.
3. Clic derecho en cada puerto → **Port Visibility → Public** (si quedan
   privados, el profesor no entra).
4. Copiar las dos URLs (`https://<algo>-8080.<region>.devtunnels.ms` y `-4200`).

### 0.3 Config del host (NO se commitea, es local del que hostea)

- `frontend/src/environments/environment.ts` → `apiUrl` a la URL del backend + `/api`:
  `apiUrl: 'https://<algo>-8080.<region>.devtunnels.ms/api'`
- `.env` → `FRONTEND_URL=` la **URL única** del frontend del túnel (URL concreta,
  **sin comodín**: el comodín rompe el redirect de OAuth). Reiniciar el backend.
- Al profesor se le pasa **la URL del frontend** (`-4200`).

### 0.4 Login en la demo remota: SIEMPRE local

Por el túnel se entra con **correo y contraseña**, no con Google (ver known
issues §6). La cuenta admin del seed da acceso a todo:

```
admin.demo@brakket.gg  /  Demo2026!
```

Google login se puede mostrar como funcionalidad en **localhost** durante la
presentación, pero por el túnel se usa el login local.

---

## 1. Inventario de módulos (qué se demuestra)

Verificado contra el código de `develop`. Esta iteración cerró casi todo lo que
en la primera estaba "próximamente".

| Módulo | Estado | En la demo |
|---|---|---|
| Landing institucional y de producto | ✅ | Se muestra |
| **Registro** local + login local + Google | ✅ | **Registro EN VIVO** (§ Sección 1) + login local |
| Dashboard `/inicio` | ✅ | Se muestra (datos del seed) |
| Juegos (catálogo, hub, tráiler, importar IGDB) | ✅ | Se muestra |
| Ligas y temporadas | ✅ | Se muestra (precargadas) |
| Torneos (formatos, bracket, resultados, disputa) | ✅ | Torneos precargados + **gran final EN VIVO** |
| Calendario de eventos (`/calendar`, RF-46) | ✅ | Se muestra **con sesión** |
| Equipos (crear, invitar, roles, perfil, stats) | ✅ | Perfil + **aceptar invitación EN VIVO** |
| Transferencias | ✅ | Opcional en vivo |
| Disputas (`/disputes`) | ✅ | Listado + deep-link a la partida |
| Historial de jugador | ✅ | Se muestra (multijuego) |
| Estadísticas (`/statistics`) | ✅ | Se muestra |
| Progresión / tienda cosmética (`/progression`) | ✅ | Se muestra |
| Notificaciones (`/notifications`) | ✅ | Se muestra |
| Perfil propio (`/profile`) | ✅ | Se muestra rápido |
| Transmisiones (`/transmisiones`, RF-35) | ✅ | Se muestra; EN VIVO si el canal transmite |
| Canal de Twitch + métricas (`/twitch`, RF-34/36) | ✅ | Se muestra (ADMIN) |
| Analítica por período (`/metricas`, RF-37) | ✅ | Series de audiencia/chat, crudas o por hora |
| **Termómetro del chat + Asistente IA (RF-39/40)** | ✅ | Sentimiento + **asistente de IA EN VIVO** |
| Reportes exportables (`/reports`, RF-50) | ✅ | Genera reporte + **descarga PDF** |
| Patrocinios y asociaciones (`/sponsorships`) | ✅ | Se muestra (precargados) |
| Panel comercial del patrocinador | ✅ | Se muestra (necesita cuenta patrocinador) |
| Administración de roles (`/admin`, RF-19) | ✅ | Se muestra (ADMIN) |

Límite honesto a narrar: el rol **ÁRBITRO** existe pero no participa del flujo de
resultados (reporta capitán → confirma rival → resuelve organizador/admin).

---

## 2. Datos que deja el seed

### 2.1 Cuentas (contraseña única: `Demo2026!`)

⚠️ Credenciales obvias a propósito; solo existen en dev.

| Cuenta | Correo | Rol / función |
|---|---|---|
| Adriana Admin | `admin.demo@brakket.gg` | **ADMIN** — /admin, /twitch, patrocinios, reportes, termómetro |
| Olivia Organizadora | `orga.demo@brakket.gg` | Comisionada de las ligas, organizadora de los torneos, resuelve resultados |
| Ana Fénix | `cap1.demo@brakket.gg` | Capitana de **Fenix Demo** |
| Bruno Lobo | `cap2.demo@brakket.gg` | Capitán de **Lobos Demo** |
| Carla Cuervo | `cap3.demo@brakket.gg` | Capitana de **Cuervos Demo** |
| Diego Titán | `cap4.demo@brakket.gg` | Capitán de **Titanes Demo** |
| Elena Kraken | `cap5.demo@brakket.gg` | Capitana de **Kraken Demo** |
| Fabián Nova | `cap6.demo@brakket.gg` | Capitán de **Nova Demo** |
| Gabriela Raptor | `cap7.demo@brakket.gg` | Capitana de **Raptors Demo** |
| Héctor Vórtex | `cap8.demo@brakket.gg` | Capitán de **Vortex Demo** |

### 2.2 Qué monta el seed

- **2 ligas** (dos juegos): *Liga Demo Brakket* → *Temporada Demo 2026* (ACTIVA), y
  *Liga Multijuego Demo Brakket* → *Temporada Multijuego Demo 2026*.
- **8 equipos** de 1 jugador, **multijuego** (juegan en los dos juegos).
- **3 torneos 1v1**:
  - **Copa Relampago (Demo)** — eliminación directa, **FINALIZADO**, campeón coronado.
  - **Masters Horizonte (Demo)** — eliminación directa en el **juego secundario**,
    **FINALIZADO** (alimenta el historial multijuego).
  - **Copa Doble Orbita (Demo)** — doble eliminación, **EN CURSO** con **solo la
    gran final reportada y sin confirmar** (el momento en vivo + aviso de
    resultado no definitivo de RF-47).
- **1 invitación de equipo pendiente** (Fenix Demo → Olivia) para que el
  dashboard muestre acciones.
- **2 patrocinadores** demo (Cafe Volcan, Nebula Energy).
- **Best-effort**: canal oficial de Twitch validado + transmisión asociada a la
  Copa Doble Orbita (si el `.env` tiene credenciales de Twitch).

> Los **IDs cambian en cada corrida**: en la demo navegá **por nombre**, nunca por
> una URL con ID memorizado. (El ID del torneo ahora se ve en su página, por si
> hace falta para asociar una transmisión.)

### 2.3 Cómo correrlo

```bash
# Requisitos: docker compose up -d (backend en localhost:8080) y Node 18+.
node scripts/seed-demo.mjs               # reset + seed (idempotente, ~30-60 s)
node scripts/seed-demo.mjs --solo-reset  # solo borrar los datos demo
```

- **Guardas**: aborta si `SPRING_PROFILES_ACTIVE=prod` o si el backend no responde.
- **Solo borra datos demo** (nombres `(Demo)` y correos `*.demo@brakket.gg`); jamás toca datos reales.
- Crea todo **vía API real** (mismas reglas de negocio que la UI), salvo el grant
  de ADMIN a `admin.demo`, que es un INSERT dev-only.
- **Prerrequisito**: tiene que haber ≥2 juegos activos en el catálogo (los siembra
  `CatalogoSeeder` desde IGDB al arrancar). Si el catálogo está vacío, el seed
  falla: entrar como admin, importar 2 juegos, y re-correr el seed.

---

## 3. Checklist previo (30 min antes, NO en escena)

1. `git pull` de `develop` con **todos los PRs de la iteración mergeados**.
2. `docker compose up -d` y verificar `http://localhost:8080/actuator/health` = UP.
3. `node scripts/seed-demo.mjs` — confirmar el resumen final con las credenciales.
4. `ng serve` en `frontend/`. **Abrir la app y verificar que `/transmisiones`
   carga**; si el bundle quedó viejo, reiniciar `ng serve` (falla conocida del watch).
5. **Túnel** (§0): forward de 8080 y 4200, ambos **Public**, `environment.ts` y
   `FRONTEND_URL` apuntando al túnel, backend reiniciado. Abrir la URL del
   frontend en **incógnito** y entrar con `admin.demo` — si entra y ves `/admin`,
   el túnel está OK.
6. **Probar desde afuera**: pedile a un compañero que abra la URL del frontend
   desde su casa. Si a él le funciona, le funciona al profesor.
7. Preparar **3 ventanas/perfiles** de navegador ya logueados (para la gran final):
   - Ventana 1 (principal): `orga.demo@brakket.gg`
   - Ventana 2: capitán del equipo A de la gran final (leerlo del bracket)
   - Ventana 3: capitán del equipo B de la gran final (puede ser incógnito)
   - Credenciales de `admin.demo` a mano (se usa al final).
8. Si se muestra el **termómetro/IA con datos reales**: dejar el canal oficial
   **en vivo con chat activo** unos minutos antes, para que el muestreo capture
   (ver §5).
9. **Reiniciar el backend** una última vez antes de empezar (limpia contadores de
   intentos de login si alguien probó de más).

---

## 4. Recorrido paso a paso

Leyenda: 🖥️ = ya está cargado, solo se muestra · 🔴 = se ejecuta EN VIVO.
Todo se hace **por la URL del túnel** (el profesor ve lo mismo).

### Sección 1 — Portada, registro y acceso (3 min)

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 1.1 🖥️ | (nadie) | Abrir la URL del frontend | Landing institucional; botón "Ver plataforma" |
| 1.2 🖥️ | (nadie) | `/producto` | Landing del producto |
| 1.3 🔴 | (nuevo) | **Registrarse en vivo** con un correo nuevo (`invitado.demo@brakket.gg`) — mostrar confirmar contraseña y el ojito de ver/ocultar | Cuenta creada, entra como JUGADOR. **El profe pidió ver el registro** |
| 1.4 🔴 | Olivia | Cerrar sesión y entrar con `orga.demo@brakket.gg` / `Demo2026!` | Entra al dashboard |

### Sección 2 — Dashboard (2 min) — Olivia

| # | Acción | Qué se debe ver |
|---|---|---|
| 2.1 🖥️ | Recorrer `/inicio` | Carrusel de juegos, rail de **próximos torneos** (Copa Doble Orbita EN_CURSO), "Tus competencias" y **Pendientes: 1 invitación de Fenix Demo** |

### Sección 3 — Juegos, liga, temporada y calendario (4 min) — Olivia

| # | Acción | Qué se debe ver |
|---|---|---|
| 3.1 🖥️ | `/games` → abrir Rocket League | Hub del juego: **tráiler embebido** (IGDB), ficha (rating, plataformas), y torneos del juego. Mencionar: importar desde IGDB e (ADMIN) crear/editar |
| 3.2 🖥️ | `/leagues` → **Liga Demo Brakket** | Detalle de la liga; Olivia es la comisionada |
| 3.3 🖥️ | Sección de temporadas | **Temporada Demo 2026** ACTIVA con cupo y formato. Narrar la jerarquía liga → temporada → torneo |
| 3.4 🖥️ | `/calendar` → filtrar por juego y liga | Los torneos demo como tarjetas de evento con fecha y estado (RF-46). ⚠️ Siempre **con sesión** |

### Sección 4 — Equipos + invitación EN VIVO (3 min) — Olivia

| # | Acción | Qué se debe ver |
|---|---|---|
| 4.1 🖥️ | `/teams` → buscar "Fenix" → perfil público | Tarjeta del club, tabs Resumen / Miembros / **Estadísticas** (winrate REAL del torneo finalizado) |
| 4.2 🔴 | Dashboard o `/teams/invitaciones` → **aceptar** la invitación de Fenix Demo | Olivia queda SUPLENTE; la plantilla de Fenix Demo muestra 2 miembros |
| 4.3 🖥️ | Perfil del equipo → historial de jugador | **Historial multijuego** (RF-15/16): el jugador aparece en los dos juegos |

### Sección 5 — Torneos: ciclo completo (4 min) — Olivia

| # | Acción | Qué se debe ver |
|---|---|---|
| 5.1 🖥️ | `/tournaments` → **Copa Relampago (Demo)** | Estado FINALIZADO, bracket completo con marcadores, camino del campeón resaltado, **campeón coronado**. (Mostrar el chip **ID N** del torneo.) |
| 5.2 🖥️ | Perfil de un equipo participante | Las estadísticas reflejan estas partidas — nada es simulado |
| 5.3 🖥️ | `/disputes` (opcional) | Listado de disputas; explicar el flujo reportar → rechazar → disputa → resuelve el organizador |

### Sección 6 — La GRAN FINAL en vivo (5 min) ⭐ — 3 ventanas

El momento estrella. Narrar: "esto no está pregrabado".

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 6.1 🖥️ | Olivia (V1) | `/tournaments` → **Copa Doble Orbita (Demo)** → bracket | **Doble eliminación**: llave superior, inferior y la **gran final dorada PENDIENTE** |
| 6.2 🖥️ | Cap A (V2) | Abrir la misma partida como capitán | Ve el **lobby con clave** (solo capitanes/organizador) |
| 6.3 🔴 | Cap A (V2) | **Reportar resultado** de la gran final (p. ej. 3–2) | Partida pasa a REPORTADA |
| 6.4 🔴 | Cap B (V3) | **Confirmar** el resultado | La partida se cierra, el bracket avanza, **campeón coronado, torneo FINALIZADO** (confetti) |
| 6.5 🖥️ | Olivia (V1) | Refrescar el bracket | Torneo cerrado. Mencionar: rechazar → disputa → la resuelve el organizador |

\* Los finalistas varían entre corridas del seed — **leerlos del bracket**, no de esta guía.

### Sección 7 — Streaming, métricas y ASISTENTE DE IA (6 min) — Admin

Entrar como `admin.demo` (o seguir con Olivia para lo público).

| # | Usuario | Acción | Qué se debe ver |
|---|---|---|---|
| 7.1 🖥️ | (público) | `/transmisiones` | Vitrina estilo Twitch: hero con el canal oficial, estado real. Offline = tarjeta con placeholder; EN VIVO = player + espectadores |
| 7.2 🖥️ | Admin | `/twitch` | Canal validado (RF-34), transmisión asociada. Bloque **Métricas de audiencia (RF-36)**: muestras/pico/promedio |
| 7.3 🖥️ | Admin | `/metricas` (Analítica, RF-37) | Elegir la transmisión: tarjetas de resumen y **curva de audiencia**. Alternar **Por hora / Muestras crudas** y acotar el rango |
| 7.4 🖥️ | Admin | **Termómetro del chat** (RF-40) | Serie de **sentimiento** del chat (barra/termómetro). Con IA si hay llave; si no, el analizador léxico determinístico |
| 7.5 🔴 | Admin | **Asistente de IA** (botón flotante en el termómetro) → preguntar p. ej. *"¿de qué habló el chat?"* o *"¿cuándo estuvo más positivo?"* | Respuesta en prosa basada en los datos reales capturados. **Si se agotó la cuota de Gemini, responde en modo degradado con los números calculados** — narrarlo como resiliencia, no como error |

> **Nota IA**: el asistente y el sentimiento trabajan sobre el **chat capturado**.
> Para que haya datos ricos, dejá el canal oficial **en vivo con chat activo**
> unos minutos antes (§3.8). Sin chat capturado, el termómetro avisa que no hay
> muestras suficientes y el asistente responde con lo que haya.

### Sección 8 — Reportes, patrocinios y administración (4 min) — Admin

| # | Acción | Qué se debe ver |
|---|---|---|
| 8.1 🔴 | `/reports` → elegir tipo (competencia/audiencia/patrocinio) → generar | Tabla del reporte en pantalla y **descarga del PDF** (RF-50) |
| 8.2 🖥️ | `/sponsorships` | Cafe Volcan y Nebula Energy; asociaciones liga/torneo; crear uno en vivo es opcional |
| 8.3 🖥️ | `/admin` | Catálogo de roles y permisos (RF-19); buscar usuario y asignar/revocar rol (en vivo: dar ÁRBITRO a un capitán y revocarlo) |
| 8.4 🖥️ | `/progression` (opcional) | Tienda cosmética: canje de puntos por cosméticos (RF-48) |
| 8.5 🖥️ | `/profile` (opcional) | Edición de perfil propio (RF-18) |

### Sección 9 — Cierre + roadmap + known issues (5 min)

**El profesor pidió explícitamente diapositivas con roadmap, completitud y known
issues.** Cerrar con:

- **Roadmap**: qué se planeó vs qué se completó en la iteración (backlog).
- **Completitud**: los RFs cubiertos (mostrar el % del backlog).
- **Known issues** (defectos encontrados y no resueltos — ver §6): decirlos de
  frente. Un defecto documentado castiga menos que uno descubierto en vivo.

### Tiempos

| Sección | Min | Acum. |
|---|---|---|
| 1. Portada, registro, acceso | 3 | 3 |
| 2. Dashboard | 2 | 5 |
| 3. Juegos, liga, temporada, calendario | 4 | 9 |
| 4. Equipos + invitación | 3 | 12 |
| 5. Torneos | 4 | 16 |
| 6. **Gran final en vivo** | 5 | 21 |
| 7. Streaming + métricas + **IA** | 6 | 27 |
| 8. Reportes, patrocinios, admin | 4 | 31 |
| 9. Cierre + roadmap + known issues | 5 | **36** |

Si hay que recortar: sacrificar 8.4/8.5 (progresión/perfil) y 5.3 (disputas).

---

## 5. El asistente de IA en detalle (RF-39/40)

Es la novedad estrella de la iteración; conviene ensayarlo aparte.

- **Dónde**: `/analytics` (Termómetro del chat) → botón flotante del asistente.
- **Qué hace**: el asistente **interpreta**; los **números los calcula Java**
  (picos, mínimos, timestamps). El modelo nunca inventa un dato del sistema.
- **Preguntas seguras para la demo**: "¿de qué se habló?", "¿cuándo estuvo más
  animado el chat?", "¿alguien mencionó [tema]?".
- **Proveedor**: Google Gemini. **Límite gratuito diario**: no lo pruebes de más
  hoy, guardá llamadas para la presentación.
- **Degradación**: sin llave o con la cuota agotada, el sentimiento cae al
  analizador léxico y el asistente responde con los números ya calculados. Nunca
  se cae. Es un punto a favor: mostrarlo como tolerancia a fallos.

---

## 6. Known issues (para la diapositiva de cierre)

Decirlos de frente el viernes:

1. **Login con Google por el túnel**: no funciona bien desde otra computadora. El
   callback de Google es una navegación del navegador que choca con la página de
   aviso del túnel. **Mitigación**: la demo remota usa **login local**; Google se
   muestra, si se quiere, en localhost. (Al profesor su Google no le daría admin
   igual, porque su correo no está en la lista.)
2. **Descripción del juego / layout de temporada**: pulido visual pendiente.
3. **Rol ÁRBITRO**: existe pero no participa del flujo de resultados.
4. **Panel comercial del patrocinador**: requiere una cuenta con perfil de
   patrocinador vinculado; con las cuentas del seed se muestra el flujo admin.

---

## 7. Puntos frágiles y plan B

| Riesgo | Síntoma | Mitigación |
|---|---|---|
| Túnel caído / VS Code cerrado | La URL deja de cargar | Mantener VS Code y el túnel abiertos toda la demo. Plan B: el host de respaldo (§0.1) |
| Se le va la luz al host | Todo cae | Host de respaldo levanta su túnel y pasa su URL |
| `ng serve` sirve bundle viejo | Rutas redirigen al landing | Reiniciar `ng serve` antes de presentar (§3.4) |
| `.env` sin `JWT_SECRET` | El backend **no arranca** (fail-fast) | Tener `JWT_SECRET` (≥32 chars) en el `.env` |
| `.env` editado sin recrear | Cambios no aplican | `docker compose up -d --force-recreate backend` (restart NO alcanza) |
| Twitch/Helix caído | `/transmisiones` muestra aviso ámbar | Narrarlo como degradación ante fallos (RNF-15), no es error |
| Cuota de Gemini agotada | El asistente responde en modo degradado | Es tolerancia a fallos: los números siguen siendo correctos. No probar de más antes |
| Confusión de ventanas en la gran final | 403 "Solo un capitán…" | Verificar la cuenta en el header. Plan B: Olivia resuelve con "Resolver" y se narra la disputa |
| Finalistas distintos a la guía | La final no es la esperada | Normal entre corridas: leer los nombres del bracket |
| Estado ensuciado ensayando | Datos inconsistentes | `node scripts/seed-demo.mjs` re-crea todo (~30-60 s). Navegar por nombre |
| Login local bloqueado | "Demasiados intentos, 15 min" | Reiniciar el backend limpia el contador (es en memoria) |

---

## 8. Reparto sugerido (5 presentadores)

> Todos deben conocer el flujo completo (el profesor puede pasar el control a
> cualquiera). Este reparto es solo el orden de conducción.

1. **Apertura** (§1–2): visión + registro en vivo + dashboard.
2. **Competencia** (§3–5): liga, temporada, equipos, torneos.
3. **El momento en vivo** (§6): gran final. Requiere ensayo con las 3 ventanas.
4. **Streaming + IA** (§7): transmisiones, métricas y el asistente de IA.
5. **Plataforma + cierre** (§8–9): reportes, patrocinios, admin, roadmap y known issues.

Ensayo recomendado: correr el seed, cronometrar una pasada completa por el túnel,
resetear y repetir. El seed existe exactamente para eso.
