# Brakket — Guía del proyecto (leer primero)

Guía práctica para que cualquier integrante del equipo abra el repo, entienda **qué
hay hasta ahora**, **cómo correrlo**, **dónde trabaja** y **cómo aporta un RF**. Es el
complemento operativo del [`PLAN-DE-TRABAJO.md`](PLAN-DE-TRABAJO.md) (reparto por
sprint) y de la *Estructura del Proyecto* oficial (PDF en esta carpeta).

## Contenido
1. [Qué es y con qué está hecho](#1-qué-es-y-con-qué-está-hecho)
2. [Arranque rápido](#2-arranque-rápido)
3. [Variables de entorno (`.env`)](#3-variables-de-entorno-env)
4. [Estructura del repositorio](#4-estructura-del-repositorio)
5. [Estado actual: qué está hecho y qué es andamiaje](#5-estado-actual-qué-está-hecho-y-qué-es-andamiaje)
6. [Cómo funciona la autenticación](#6-cómo-funciona-la-autenticación)
7. [Dónde trabaja cada quien](#7-dónde-trabaja-cada-quien)
8. [Cómo agregar un RF (patrón de referencia)](#8-cómo-agregar-un-rf-patrón-de-referencia)
9. [Flujo Git y CI](#9-flujo-git-y-ci)
10. [Problemas comunes](#10-problemas-comunes)

---

## 1. Qué es y con qué está hecho

Plataforma web para **gestión y transmisión de ligas y torneos de esports**. Monorepo
con dos aplicaciones que espejan los mismos módulos de dominio.

| Capa | Tecnología | Puerto local |
|------|-----------|--------------|
| Frontend | Angular 19 (standalone) | `4200` |
| Backend | Java 21 + Spring Boot 3.5 (Maven) | `8080` |
| Base de datos | PostgreSQL 16 + Flyway | `5432` |
| Auth | Google OAuth 2.0 → JWT propio | — |
| Orquestación | Docker / docker-compose | — |

**Requisitos según cómo lo corras:**
- **Solo Docker + Node 22** → si levantás el backend con docker-compose (recomendado
  para probar rápido; no necesitás Java ni Maven instalados).
- **JDK 21 + Node 22** → si además querés correr el backend desde el IDE / Maven.

---

## 2. Arranque rápido

```bash
git clone <URL-del-repo> brakket
cd brakket
cp .env.example .env          # y completar (ver sección 3)
```

**Opción A — todo con Docker (más simple para probar):**
```bash
docker compose up --build -d      # levanta db + backend
cd frontend && npm install && npm start
```

**Opción B — backend desde el IDE/Maven (para desarrollar backend):**
```bash
docker compose up -d db           # solo la base de datos
cd backend && ./mvnw spring-boot:run     # Windows: mvnw.cmd spring-boot:run
cd ../frontend && npm install && npm start
```

Luego abrí **http://localhost:4200**. Otras URLs útiles:
- API health: http://localhost:8080/actuator/health
- Swagger (documentación de la API): http://localhost:8080/swagger-ui.html

> ⚠️ El backend usa `ddl-auto: validate`: **Flyway es dueño del esquema** y Hibernate
> solo valida que las entidades calcen. Si agregás una tabla/columna, se hace con una
> **migración nueva** en `backend/src/main/resources/db/migration` (`V3__...sql`), no
> tocando `V1`.

---

## 3. Variables de entorno (`.env`)

Se copian de `.env.example` a `.env` en la raíz. **El `.env` está en `.gitignore`: no se
sube.** docker-compose las lee y se las pasa al backend.

| Variable | Para qué | ¿Hay que ponerla? |
|----------|----------|-------------------|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Credenciales de Postgres | No, defaults sirven (`brakket`/`brakket`/`brakket`) |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Conexión del backend fuera de Docker | No, defaults sirven |
| `FRONTEND_URL` | Origen permitido para CORS | No (default `http://localhost:4200`) |
| **`GOOGLE_CLIENT_ID`** | Login con Google | **Sí — sin esto el login no funciona** |
| **`GOOGLE_CLIENT_SECRET`** | Login con Google | **Sí** |
| `JWT_SECRET` | Firma del token propio (mín. 32 chars) | Sí, uno compartido por el equipo |
| `JWT_EXPIRATION_MS` | Duración del token | No (default 24 h) |
| `TWITCH_*` | Integración Twitch (EPIC-10) | Aún no (sprints posteriores) |
| `AI_*` | Análisis de sentimiento (EPIC-10) | Aún no |

**Para que TODO el equipo pueda probar el login** hay dos formas:

- **(Recomendada) Un solo client de Google compartido:** el coordinador comparte los
  valores de `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` y un `JWT_SECRET` común por un
  canal privado (no en el repo). **Además**, cada integrante debe estar agregado como
  *Test user* en la pantalla de consentimiento de Google (ver abajo), porque la app está
  en modo *Testing*.
- **(Alternativa) Cada quien su propio client:** cada uno crea sus credenciales en Google
  Cloud (ver `.env.example`, sección Google) con la misma redirect URI
  `http://localhost:8080/login/oauth2/code/google`.

> La redirect URI es siempre `http://localhost:8080/login/oauth2/code/google` (igual para
> todos, porque todos corren en localhost). No hay que configurar nada por-persona salvo
> ser *Test user*.

---

## 4. Estructura del repositorio

```
brakket/
├── backend/            API Spring Boot (Java 21, Maven)
├── frontend/           SPA Angular 19
├── docs/               Documentación (esta guía, plan de trabajo, PDFs académicos)
├── .github/            CI (workflows) + plantilla de PR + CODEOWNERS
├── docker-compose.yml  Postgres + backend
└── .env.example        Plantilla de variables de entorno
```

### Backend — `backend/src/main/java/com/coffeecommits/brakket/`

Organizado **por módulo de dominio**. Cada módulo repite el mismo patrón interno:

```
<modulo>/
├── controller/   # Expone la API REST (@RestController)
├── service/      # Lógica de negocio (interface + Impl)
├── repository/   # Acceso a datos (Spring Data JPA)
├── model/        # Entidades JPA (mapean al esquema Flyway)
└── dto/          # Objetos de entrada/salida de la API
```

Módulos existentes (todos con sus entidades y repositorios ya creados):
`auth`, `team`, `game`, `league`, `tournament`, `dispute`, `twitch`, `analytics`,
`sponsorship`, `notification`, `progression`, `statistics`, `admin`.

Transversales:
- `config/` — seguridad, JWT, CORS, OAuth2, Swagger.
- `common/` — `ApiResponse`, excepciones + `GlobalExceptionHandler`, utilidades.
- `resources/db/migration/` — migraciones Flyway (`V1__init_schema.sql` = 26 tablas,
  `V2__seed_roles.sql` = roles base).

### Frontend — `frontend/src/app/`

```
app/
├── core/         # Singletons: guards, interceptors, services (auth, api, token)
├── shared/       # Reutilizable: button, modal, table, pipes, directives
├── layout/       # Header, sidebar, footer, layout
├── models/       # Interfaces TypeScript
└── features/     # Un módulo por dominio (lazy-loaded), espeja el backend
```

Cada feature: `<feature>.routes.ts` + `pages/` + `components/` + `services/`. Cada
componente tiene 4 archivos: `.ts`, `.html`, `.scss`, `.spec.ts`.

---

## 5. Estado actual: qué está hecho y qué es andamiaje

### ✅ Listo y verificado
- **Infraestructura completa y corriendo**: monorepo, Docker, CI, esquema de BD (26
  tablas), las 26 entidades JPA validan contra el esquema al arrancar.
- **RF-17 — Login con Google (EPIC-01)**: flujo completo funcionando. Iniciás sesión con
  Google → el backend **crea/actualiza tu usuario** en la BD y le asigna rol base
  `JUGADOR` → emite un JWT → el frontend lo guarda y quedás autenticado.
- **`GET /api/me`**: devuelve el usuario autenticado (nombre, correo, foto, roles).
- **Pantalla "Mi perfil"**: muestra tus datos reales de `/api/me`.
- **Seguridad**: rutas `/api/**` sin token devuelven `401`; guards de frontend
  (`authGuard`, `roleGuard`) funcionando.

### 🚧 Andamiaje (esto es el trabajo de los sprints)
- **Todas las demás pantallas** del frontend son placeholders ("Pendiente EPIC-XX").
- **La mayoría de módulos del backend** tienen entidad + repositorio, pero **aún no**
  tienen `controller`/`service`/`dto` (están como carpetas vacías con `.gitkeep`).

El módulo **`auth`** es el **ejemplo vivo del patrón** a copiar (ver sección 8).

---

## 6. Cómo funciona la autenticación

1. El usuario hace clic en *Iniciar sesión* → el frontend navega a
   `http://localhost:8080/oauth2/authorization/google`.
2. Spring Security redirige a Google; el usuario elige su cuenta.
3. Google vuelve a `http://localhost:8080/login/oauth2/code/google`.
4. `OAuth2LoginSuccessHandler` (backend) **da de alta/actualiza el usuario** con rol base,
   genera un **JWT** y redirige a `http://localhost:4200/auth/callback?token=...`.
5. El componente `callback` (frontend) guarda el token en `localStorage` y llama a
   `GET /api/me`.
6. En cada request siguiente, el `jwtInterceptor` envía `Authorization: Bearer <jwt>` y el
   `JwtAuthenticationFilter` (backend) lo valida.

Piezas clave: `config/SecurityConfig`, `config/OAuth2LoginSuccessHandler`,
`config/JwtService`, `config/JwtAuthenticationFilter` (backend);
`core/services/auth.service.ts`, `core/services/token.service.ts`,
`core/interceptors/jwt.interceptor.ts`, `features/auth/` (frontend).

---

## 7. Dónde trabaja cada quien

El reparto por RF del Sprint 1 está en [`PLAN-DE-TRABAJO.md`](PLAN-DE-TRABAJO.md). En
términos de carpetas, **cada RF toca su módulo en backend y su feature en frontend**:

| Área | Backend | Frontend |
|------|---------|----------|
| Auth / perfil / roles | `auth/` | `features/auth/`, `features/profile/` |
| Equipos y plantillas | `team/` | `features/teams/` |
| Catálogo de juegos | `game/` | `features/games/` |
| Ligas | `league/` | `features/leagues/` |
| Torneos | `tournament/` | `features/tournaments/` |
| Disputas | `dispute/` | `features/disputes/` |
| Twitch / IA | `twitch/`, `analytics/` | `features/twitch/`, `features/analytics/` |
| Patrocinios | `sponsorship/` | `features/sponsorships/` |
| Notif. / stats / progresión / admin | `notification/`, `statistics/`, `progression/`, `admin/` | features homónimas |

> **Regla de oro:** un RF = una rama = un PR. Como el reparto es *fullstack por módulo*,
> cada quien hace el backend y el frontend de su RF para minimizar conflictos.

---

## 8. Cómo agregar un RF (patrón de referencia)

Mirá el módulo **`auth`** como plantilla. Para un RF nuevo:

**Backend** (en `<modulo>/`):
1. `dto/` → un `record` de entrada y/o salida (ej. `UsuarioResponse`).
2. `service/` → interface + `Impl` con `@Service` y la lógica (`@Transactional`).
3. `controller/` → `@RestController @RequestMapping("/api")` con los endpoints.
4. `test/.../<modulo>/` → una prueba JUnit 5 + Mockito del service (obligatorio).
5. Si hace falta esquema nuevo: migración `V#__descripcion.sql` en `db/migration`.

**Frontend** (en `features/<feature>/`):
1. `services/<feature>.service.ts` → consume la API vía `ApiService`.
2. `pages/<algo>/` → componente (4 archivos, incluido `.spec.ts`).
3. `<feature>.routes.ts` → registrar la ruta.
4. Si es una sección nueva del menú, agregar el enlace en
   `layout/sidebar/sidebar.component.ts`.

**Convenciones:** Java `PascalCase`, Angular `kebab-case`, BD `snake_case`. Colores de
marca: azul `#2563EB`, cian `#22D3EE` (definidos en `assets/styles/_variables.scss`).

**Contrato de API:** en éxito, los endpoints pueden devolver el DTO directo (como
`/api/me`); en error, el `GlobalExceptionHandler` responde con `ApiResponse`
(`{success, message, ...}`) y el código HTTP correcto (404, 409, 400…).

---

## 9. Flujo Git y CI

- Ramas: `main` (estable) ← `develop` (integración) ← `feature/RF-XX-...`.
- Todo cambio entra por **Pull Request a `develop`** con al menos **1 revisión**.
- Commits con la clave de Jira: `SCRUM-XX RF-YY descripción corta`.
- Al abrir un PR se completa la **plantilla** (`.github/pull_request_template.md`) con su
  Definición de Hecho.
- **CI** (`.github/workflows/`): en cada push/PR corre el build del backend y del frontend
  por separado según qué carpeta cambió.
- **Definición de Hecho:** compila, pasa CI, cubre los criterios de la ERS, tiene su
  prueba (JUnit / `.spec.ts`), revisado y mergeado a `develop`.

---

## 10. Problemas comunes

| Síntoma | Causa / solución |
|---------|------------------|
| `redirect_uri_mismatch` al entrar con Google | La redirect URI en Google Cloud debe ser exactamente `http://localhost:8080/login/oauth2/code/google` (sin barra final, `http`, no `https`). |
| Google dice *access_denied* / *app en pruebas* | Tu correo no está como **Test user** en la pantalla de consentimiento. Pedile al dueño del client que te agregue. |
| `Conflict. The container name "/brakket-db" is already in use` | Ya hay un contenedor con ese nombre corriendo. `docker rm -f brakket-db` y volvé a hacer `docker compose up -d`. |
| `/api/...` devuelve `401` | Falta el header `Authorization: Bearer <jwt>` (no iniciaste sesión) o el token venció. |
| El nombre no aparece tras recargar | Debe recargarse solo (el `AuthService` recupera el perfil al iniciar si hay token). Si no, revisá que el backend esté arriba y `/api/me` responda. |
| El backend no arranca: *Schema validation* | Una entidad JPA no calza con el esquema Flyway. Revisá la entidad o agregá la migración correspondiente. |

---

*Última actualización: sprint 1. Mantené esta guía al día cuando cambie la forma de
correr o probar el proyecto.*
