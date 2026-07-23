# Brakket — Guion de presentación y estructura del proyecto

## 1. Qué es Brakket (30 seg)

Plataforma web de gestión de ligas y torneos de esports, inspirada en
Challenger Mode: cualquier persona entra, elige un juego del catálogo y
puede competir con su equipo u organizar sus propias competencias. Los
administradores curan el catálogo y moderan la plataforma.

**Stack**: Angular 19 (standalone components + signals) · Spring Boot 3.5
/ Java 21 · PostgreSQL 16 · Flyway (24 migraciones) · Docker Compose ·
integración con la API de RAWG (catálogo de videojuegos) · Google OAuth2
y login local, ambos emitiendo el mismo JWT propio.

## 2. Arquitectura (1 min)

Monorepo con tres piezas:

```
Brakket/
├── frontend/   Angular SPA (puerto 4200)
├── backend/    API REST Spring Boot (puerto 8080)
├── docs/       ERS, plan, decisiones de diseño, guiones
└── docker-compose.yml  (db + backend)
```

Flujo de una petición: SPA → JWT en header → `JwtAuthenticationFilter`
valida y carga roles/permisos reales desde la base → `@PreAuthorize` en
controllers → services con la lógica y validaciones → JPA/Postgres.
Flyway es dueño del esquema (Hibernate solo valida).

## 3. Backend — módulos (`com.coffeecommits.brakket`)

| Módulo | Qué hace | RFs |
|---|---|---|
| `auth` | Google OAuth2 → JWT propio; login/registro local con BCrypt; perfil `/me`; roles y permisos desde BD; bootstrap de ADMIN para correos del equipo (solo vía Google) | RF-17, RF-18 |
| `admin` | Asignar/revocar roles por usuario, catálogo de roles y permisos | RF-19 |
| `game` | Catálogo de juegos autopoblado desde RAWG (seeder al arranque + búsqueda e importación en vivo); perfil competitivo por juego como curaduría opcional | RF-20, RF-21 |
| `league` | Ligas (crear/editar/eliminar, foto, descripción, reglas) y temporadas; el creador queda como comisionado de SU liga | RF-22, RF-23 |
| `tournament` | Torneos con modelo abierto: comunitarios (solo juego) o de liga (temporada); wizard valida juego activo, fechas, cupos y límites del perfil competitivo; inscripción de capitanes; público/privado; premio | RF-24, RF-25 |
| `team` | Registro, edición y disolución de equipos; búsqueda pública; perfil público; invitaciones; roles de plantilla | RF-01–09 |
| `transfer` | Solicitud y respuesta de transferencias; historial automático | RF-12–14 |
| `common` / `config` | Seguridad (SecurityConfig, JwtService), CORS, manejo de excepciones, Swagger | RNF |

Tablas ya modeladas sin API todavía (deuda declarada): disputas,
partidas/brackets, notificaciones, patrocinios, métricas de Twitch y
sentimiento, logros.

## 4. Frontend — estructura (`frontend/src/app`)

| Carpeta | Qué contiene |
|---|---|
| `core/` | `AuthService` (sesión, roles, login Google y local), `ApiService`, `TokenService`, `ThemeService` (claro/oscuro), guards (`authGuard`, `roleGuard` que espera `/me`), interceptors (JWT, errores 401) |
| `layout/` | Shell: `sidebar` (navegación filtrada por rol), `topbar` (buscador global de ligas/equipos/juegos, campana, avatar con rol), `footer` |
| `features/home` | Dashboard: héroe con juego destacado, fila "Juegos top", rail de próximos torneos, pendientes accionables, gestión (admin) |
| `features/games` | Catálogo escaparate (marquesinas + pósters + búsqueda en vivo contra RAWG con "+ Agregar"), hub público del juego (banner, tabs Descripción/Torneos, menú Crear), formulario admin con buscador RAWG, perfil competitivo |
| `features/leagues` | Lista con portadas, detalle con banner/reglas/temporadas/zona de peligro, formulario con foto y campos RF-22 |
| `features/tournaments` | Lista global con filtros 1v1…5v5, detalle con tabs Resumen/Equipos e inscripción, wizard de 3 pasos (General → Equipos → Fecha + config avanzada), tarjeta reutilizable |
| `features/teams` | Lista/búsqueda, formulario, plantilla (roster), invitaciones, perfil público |
| `features/transfers` | Bandejas de pendientes/enviadas y formulario de solicitud |
| `features/profile` / `admin` | Perfil propio; panel de roles (RF-19) |
| `features/*` (pronto) | disputes, statistics, progression, notifications, twitch, analytics, sponsorships — placeholders honestos |
| `shared/` | `page-header`, `empty-state`, `coming-soon`, `utils/cover.ts` (cadena de portadas: foto propia → arte del juego → stock → gradiente) |
| `models/` | Interfaces TypeScript espejo de los DTOs |

Sistema de diseño: `DESIGN.md` normativo (tokens naranja #ff5500, Barlow,
escala tipográfica y de radios), tema claro/oscuro, contraste AA.

## 5. Guion hablado (10–12 min)

1. **Apertura (1 min)** — Problema: organizar competencias amateurs es
   caos de Discord y hojas de cálculo. Brakket lo centraliza. Mostrar el
   dashboard.
2. **Arquitectura (1–2 min)** — Diapositiva/diagrama con el flujo
   SPA → JWT → API → Postgres. Mencionar Flyway, tests (123 backend +
   57 frontend) y peer review por PRs.
3. **Demo (6–8 min)** — Seguir `docs/guion-demo.md` (4 actos):
   visitante sin sesión → usuario se registra (login local) y crea un
   torneo con el wizard → capitán inscribe a su equipo → admin cura el
   catálogo y modera. Punto fuerte: el catálogo se puebla solo desde la
   API de RAWG con el arte oficial.
4. **Decisiones de diseño (1 min)** — `docs/decisiones-diseno.md`:
   modelo abierto de organizadores (DD-01) como desviación consciente de
   la ERS, con la habilitación por admin como configuración futura;
   perfil competitivo como curaduría (DD-03); login local (DD-04).
5. **Cierre (30 seg)** — Deuda priorizada: brackets (RF-26/27),
   notificaciones, disputas, analítica de patrocinio. Roadmap claro.

## 6. Comandos para levantar todo

Requisitos: Docker Desktop corriendo, Node 20+, `.env` en la raíz
(copiar de `.env.example`; necesita credenciales de Google y
`RAWG_API_KEY`).

```bash
# Base de datos + backend (desde la raíz del repo)
docker compose up -d --build

# Solo la base (si vas a correr el backend fuera de Docker)
docker compose up -d db
cd backend && ./mvnw.cmd spring-boot:run

# Frontend
cd frontend
npm install          # solo la primera vez
npx ng serve         # → http://localhost:4200

# Ver logs del backend
docker compose logs -f backend

# Tests
cd backend && ./mvnw.cmd test
cd frontend && npx ng test --watch=false --browsers=ChromeHeadless
```

Notas operativas:
- Cambios en `.env` requieren `docker compose up -d --force-recreate backend`
  (un restart no relee variables).
- No correr `ng build`/`ng test` con `ng serve` levantado (esbuild falla).
- La API queda en `http://localhost:8080` (Swagger en `/swagger-ui.html`).
- Cuenta demo de jugador: `demo@brakket.gg` / `demo-brakket-1`.
