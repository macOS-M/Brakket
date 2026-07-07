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

## Distribución por sprint (según Roadmap del diseño)

| Sprint | Cierre | Épicas | RF |
|--------|--------|--------|-----|
| 1 | 12/06 | EPIC-01 Autenticación y Perfiles | RF-17 a RF-19 |
| 2 | 19/06 | EPIC-06 Catálogo · EPIC-07 Ligas · EPIC-08 (inicio) | RF-20 a RF-25 |
| 3 | 26/06 | EPIC-02/03/04 Equipos · EPIC-08 Motor (cierre) | RF-01 a RF-14, RF-26 a RF-28 |
| 4 | 03/07 | EPIC-05 Historial · EPIC-09 Disputas · EPIC-12 Notif./Calendario | RF-15,16, RF-29 a RF-33, RF-45,46 |
| 5 | 10/07 | EPIC-10 Twitch/IA · EPIC-13 Estadísticas (inicio) | RF-34 a RF-40, RF-47 |
| 6 | 17/07 | EPIC-11 Patrocinios · EPIC-14 Admin · EPIC-13 (cierre) | RF-41 a RF-44, RF-48 a RF-50 |

> ⚠️ **Sprint 3 es el más cargado (65 pts).** Adelantar trabajo en semanas de menor
> carga académica (mitigación del riesgo R-06).

## Sprint 1 — arranque conjunto (todos)

El primer sprint es transversal: **todo el equipo trabaja sobre EPIC-01** porque habilita
el resto. Sugerencia de reparto:

- **Backend auth (RF-17 login Google, RF-19 roles):** Matías + Camilo.
- **Backend perfil (RF-18) y modelo de usuario:** Derek Carmiol.
- **Frontend login + guard + layout + perfil:** Gabriel.
- **Infra: docker-compose, CI, variables de entorno, despliegue:** Marcos.
- **Pruebas de humo del ciclo login → /me → logout y revisión de PRs:** Dereck.

Meta de cierre de Sprint 1: un usuario puede iniciar sesión con Google, se crea su
usuario con rol base, y las rutas protegidas del frontend lo redirigen a login.

## Cómo empezar (cada integrante)

1. Clonar el repo `brakket` y hacer `git switch develop`.
2. Backend: `cp .env.example .env`, completar credenciales, `docker compose up -d db`,
   `./mvnw spring-boot:run`.
3. Frontend: `npm install`, `npm start`.
4. Tomar un RF del sprint en Jira, crear `feature/RF-XX-...`, implementar, abrir PR.

## Objetivo global (ERS)

Demostrar un **torneo de prueba con ≥4 equipos**: inscripción → generación de fixtures →
reporte de resultados → tabla/bracket actualizados, con roles y disputas funcionando.
