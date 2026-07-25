# Brakket — Plan de trabajo del equipo

Guía operativa para construir Brakket entre los 6 integrantes de Coffee&Commits a lo
largo de los 6 sprints. Se basa en el Roadmap del Informe de Diseño y en la ERS.

## Repositorio (monorepo)

Un solo repositorio `brakket` con dos carpetas:

| Carpeta | Contenido |
|---------|-----------|
| `backend/`  | API REST (Spring Boot, PostgreSQL, Flyway, OAuth, Twitch/IA) |
| `frontend/` | SPA (Angular 19) |

Ramas: `main` (estable) ← `develop` (integración) ← `feature/RF-XX-...`.
Todo cambio entra por Pull Request a `develop` con al menos **1 revisión** (calidad).
La CI corre por separado según la carpeta modificada (filtros de ruta en `.github/workflows/`).

## Convenciones

- **Commits:** `SCRUM-XX RF-YY descripción corta` (enlaza a Jira).
- **Ramas:** `feature/RF-01-registrar-equipo`, `fix/RF-26-bracket-impar`.
- **Un RF = una rama = un PR.** En una misma rama pueden ir los cambios de `backend/`
  y `frontend/` del mismo RF, o separarlos en dos PRs si conviene revisarlos aparte.
- **Definición de "Hecho":** compila, pasa CI, criterios de aceptación de la ERS
  cubiertos, revisado y mergeado a `develop`.

## Roles del equipo

| Integrante | Rol | Enfoque sugerido |
|-----------|-----|------------------|
| Camilo Céspedes | Coordinador General | Arquitectura, integración, panel admin |
| Matías Calvo | Coordinador de Desarrollo | Motor de competencias, backend core |
| Derek Carmiol | Coordinador de Desarrollo | Twitch/IA, backend integraciones |
| Dereck Chavarría | Coordinador de Calidad | Pruebas, revisión de PRs, disputas |
| Gabriel Valverde | Coordinador de Calidad | Pruebas, frontend, estadísticas |
| Marcos Morales | Coordinador de Soporte | Infra (Docker/CI), notificaciones, soporte |

> La asignación es **fullstack por módulo**: quien toma un RF hace su backend y su
> frontend, para minimizar dependencias entre personas y conflictos de merge.

## Distribución por sprint

La planeación de referencia del **Informe de Diseño** distribuye las 14 épicas así:

| Sprint | Cierre | Épicas | RF |
|--------|--------|--------|-----|
| 1 | 13/07 | EPIC-01 Auth · EPIC-06 Catálogo · EPIC-02/03 Equipos · EPIC-07 Ligas · EPIC-08 Torneos (inicio) | *(ver detalle abajo — re-planificado en Jira)* |
| 2 | 20/07 | EPIC-06 Catálogo · EPIC-07 Ligas · EPIC-08 (inicio) | RF-20 a RF-25 |
| 3 | 27/07 | EPIC-02/03/04 Equipos · EPIC-08 Motor (cierre) | RF-01 a RF-14, RF-26 a RF-28 |
| 4 | 03/08 | EPIC-05 Historial · EPIC-09 Disputas · EPIC-12 Notif./Calendario | RF-15,16, RF-29 a RF-33, RF-45,46 |
| 5 | 10/08 | EPIC-10 Twitch/IA · EPIC-13 Estadísticas (inicio) | RF-34 a RF-40, RF-47 |
| 6 | 17/08 | EPIC-11 Patrocinios · EPIC-14 Admin · EPIC-13 (cierre) | RF-41 a RF-44, RF-48 a RF-50 |

> ⚠️ **Sprint 3 es el más cargado (65 pts).** Adelantar trabajo en semanas de menor
> carga académica (mitigación del riesgo R-06).
>
> ℹ️ **El Sprint 1 fue re-planificado en Jira** respecto al roadmap del diseño (que solo
> contemplaba EPIC-01, RF-17–19, 16 pts). El tablero real adelanta trabajo de las épicas
> de catálogo, equipos, ligas y torneos, subiendo el Sprint 1 a **44 pts**. Sprints 2–6
> se re-planificarán con la velocidad real medida al cierre de cada sprint.

## Sprint 1 — arranque (6 – 13 jul, 44 pts, 12 historias)

Sprint 1 no es solo autenticación: además de habilitar EPIC-01 (auth), el equipo adelanta
el **catálogo de juegos**, la **gestión de equipos**, la **creación de ligas** y la
**creación de torneos**, para llegar antes al objetivo de un torneo demostrable. Historias
del tablero (clave SCRUM):

| SCRUM | RF | Historia | Épica | Pts |
|-------|-----|----------|-------|-----|
| 51 | RF-17 | Proveedor Google OAuth + base de datos de usuarios | EPIC-01 Autenticación | 8 |
| 53 | RF-19 | Control de roles y permisos | EPIC-01 Autenticación | 5 |
| 52 | RF-18 | Administrar perfil de usuario | EPIC-01 Autenticación | 3 |
| 84 | RF-20 | Gestionar catálogo de juegos | EPIC-06 Catálogo | 3 |
| 54 | RF-01 | Registrar equipo | EPIC-02 Gestión de Equipos | 3 |
| 55 | RF-02 | Editar equipo | EPIC-02 Gestión de Equipos | 2 |
| 56 | RF-03 | Disolver equipo | EPIC-02 Gestión de Equipos | 2 |
| 57 | RF-04 | Consultar perfil del equipo | EPIC-02 Gestión de Equipos | 2 |
| 58 | RF-05 | Buscar equipos | EPIC-02 Gestión de Equipos | 3 |
| 76 | RF-09 | Asignar rol a integrante | EPIC-03 Gestión de Plantilla | 3 |
| 86 | RF-22 | Crear y configurar liga | EPIC-07 Ligas y Temporadas | 5 |
| 88 | RF-24 | Crear torneo asociado a juego | EPIC-08 Torneos e Inscripciones | 5 |

> Nota: en Jira RF-24 aparece etiquetado como EPIC-10; según el Informe de Diseño "Crear
> torneo" pertenece a EPIC-08. Conviene alinear la etiqueta en el tablero.

**Sugerencia de reparto** (fullstack por módulo, ~equilibrado por puntos):

| Integrante | Historias | Pts | Módulos que toca |
|-----------|-----------|-----|------------------|
| Matías Calvo | RF-17 | 8 | `auth/` (OAuth Google, JWT, alta de usuario + rol base) |
| Camilo Céspedes | RF-19, RF-20 | 8 | `auth/` (roles/permisos), `game/` (catálogo) |
| Derek Carmiol | RF-22, RF-24 | 10 | `league/`, `tournament/` (crear liga → temporada → torneo) |
| Dereck Chavarría | RF-01, RF-02, RF-03 | 7 | `team/` (alta/edición/baja de equipo) |
| Marcos Morales | RF-04, RF-05, RF-09 | 8 | `team/` (consulta/búsqueda/plantilla) + infra (Docker/CI/`.env`) |
| Gabriel Valverde | RF-18 | 3 | `profile/` + frontend transversal (login, callback, guards, layout) |

> **Coordinación del módulo `team/`:** Dereck y Marcos comparten el módulo de equipos.
> Acordar que el dueño del andamiaje base (entidad/repositorio/DTO comunes) mergee primero
> a `develop` para minimizar conflictos; el resto parte de esa base.
>
> Gabriel lleva menos puntos de historia porque además sostiene el **frontend transversal**
> (flujo de login → callback → guard → layout) del que dependen las pantallas de todos.

**Meta de cierre de Sprint 1:** un usuario inicia sesión con Google (se crea su usuario con
rol base), gestiona su perfil, y existe el catálogo de juegos, la creación/edición de
equipos con plantillas, y la creación de ligas y torneos asociados a un juego.

## Cómo empezar (cada integrante)

1. Clonar el repo `brakket` y hacer `git switch develop`.
2. Backend: `cp .env.example .env`, completar credenciales, `docker compose up -d db`,
   `./mvnw spring-boot:run`.
3. Frontend: `npm install`, `npm start`.
4. Tomar un RF del sprint en Jira, crear `feature/RF-XX-...`, implementar, abrir PR.

## Objetivo global (ERS)

Demostrar un **torneo de prueba con ≥4 equipos**: inscripción → generación de fixtures →
reporte de resultados → tabla/bracket actualizados, con roles y disputas funcionando.
