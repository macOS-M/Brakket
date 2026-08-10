# Brakket — Backend

API REST de **Brakket**, plataforma de gestión y transmisión de ligas y torneos de esports
(proyecto del curso Proyecto de Ingeniería de Software 3, equipo Coffee&Commits).

## Stack

- **Java 21** + **Spring Boot 3.5**
- **Maven** (con wrapper `mvnw`)
- **PostgreSQL 16** + **Flyway** (migraciones)
- **Spring Security** + **OAuth2 (Google) → JWT** propio
- **springdoc-openapi** (Swagger UI en `/swagger-ui.html`)
- **Docker** / docker-compose
- WebSocket (notificaciones en tiempo real)

## Arquitectura

Arquitectura en capas, organizada **por módulo de dominio** (cumple RNF-09, separación
de responsabilidades). Paquete base `com.coffeecommits.brakket`. Cada módulo contiene
sus propios `controller/` `service/` `repository/` `model/` (entidades) `dto/`.

```
com.coffeecommits.brakket
├── config/          SecurityConfig, CorsConfig, SwaggerConfig, Jwt*, propiedades Twitch/IA
├── common/          exception/ · dto/ (ApiResponse) · util/ · web/ (health)
├── auth/            EPIC-01  Autenticación (Google→JWT) y roles
├── team/            EPIC-02/03/04  Equipos, plantillas y transferencias
├── league/          EPIC-07  Ligas y temporadas
├── game/            EPIC-06  Catálogo de juegos
├── tournament/      EPIC-08  Torneos, fixtures, brackets, inscripciones (Match)
├── dispute/         EPIC-09  Resultados y disputas
├── twitch/          EPIC-10  Integración Twitch (captura)
├── analytics/       EPIC-10  Análisis de sentimiento (IA)
├── sponsorship/     EPIC-11  Patrocinadores y publicidad
├── notification/    EPIC-12  Notificaciones
├── statistics/      EPIC-13  Estadísticas e historial
├── progression/     EPIC-13  Progresión y logros
└── admin/           EPIC-14  Panel administrativo + auditoría
```

> Convenciones del equipo: clases Java en **PascalCase**, base de datos en **snake_case**
> (las tablas siguen el diccionario de la ERS, en español; las entidades las mapean).

### Autenticación (EPIC-01)
Login con **Google (OAuth2)**; al completarse, `OAuth2LoginSuccessHandler` emite un
**JWT** y redirige al frontend (`/auth/callback?token=...`). La SPA guarda el token y lo
envía en cada request (`Authorization: Bearer`), validado por `JwtAuthenticationFilter`
(sesión *stateless*). Falta (TODO EPIC-01): persistir el usuario y sus roles en la BD.

El **esquema de base de datos** (26 tablas del diccionario de datos) vive en
`src/main/resources/db/migration/` como migraciones Flyway. **Nunca** se modifica una
migración ya aplicada: se crea una nueva `V<n>__descripcion.sql`.

## Requisitos

- JDK 21
- Docker Desktop (para la base de datos)
- (Opcional) Maven; si no, usar el wrapper `./mvnw`

## Arranque rápido (local)

> `docker-compose.yml` y `.env.example` están en la **raíz del monorepo** (un nivel arriba).

```bash
# desde la raíz del repo:
cp .env.example .env            # completar credenciales

# opción A — todo en Docker (BD + backend)
docker compose up --build

# opción B — solo la BD en Docker y el backend con Maven (recomendado para desarrollar)
docker compose up -d db
cd backend && ./mvnw spring-boot:run   # Windows: mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080`. Pruebas de humo:

```bash
curl http://localhost:8080/api/public/ping     # {"status":"ok","app":"brakket-backend"}
# Swagger UI:  http://localhost:8080/swagger-ui.html
```

Al arrancar, Flyway crea automáticamente las 26 tablas y siembra los roles base.
El perfil activo por defecto es `dev` (`application-dev.yml`).

## Configuración (variables de entorno)

Ver `.env.example` en la raíz. Claves: base de datos (`DB_*` / `POSTGRES_*`),
Google OAuth (`GOOGLE_CLIENT_ID/SECRET`), **JWT** (`JWT_SECRET`), Twitch (`TWITCH_*`)
e IA (`AI_*`). Las integraciones externas están encapsuladas en `config/TwitchProperties`
y `config/AiProperties` (RNF-23).

Sin `AI_API_KEY` el análisis de sentimiento del chat (RF-39) **igual funciona**: cae
al analizador léxico, que es determinista y no sale a la red. Con la llave, cada
ventana de chat que captura RF-38 se clasifica con el modelo, y si el proveedor
falla se vuelve al léxico sin perder la muestra.

## Comandos útiles

```bash
./mvnw clean package         # compilar y empaquetar
./mvnw test                  # pruebas
./mvnw spring-boot:run       # ejecutar
```

## Flujo de trabajo Git

- Ramas: `feature/RF-XX-descripcion` desde `develop`.
- PR obligatorio hacia `develop` con al menos una revisión.
- Commits referenciando la clave de Jira, p. ej. `SCRUM-54 RF-01 registrar equipo`.
- `main` es la rama estable (solo merges de `develop` revisados).

## Cómo agregar una funcionalidad (RF-XX)

1. En el módulo correspondiente, crear las clases en `controller/`, `service/` y `dto/`
   (las carpetas ya existen con un `.gitkeep`).
2. Usar el `Repository` de la entidad (ya generado en `<modulo>/repository`); las
   entidades están en `<modulo>/model`.
3. Si se necesita una tabla o columna nueva, crear una migración Flyway `V<n>__...sql`
   y actualizar la entidad JPA (nunca editar una migración ya aplicada).
4. Validar entradas con `jakarta.validation`, devolver `ApiResponse<T>` y mapear
   errores con el `GlobalExceptionHandler` (`BusinessException` → 409).
