# Brakket — Backend

API REST de **Brakket**, plataforma de gestión y transmisión de ligas y torneos de esports
(proyecto del curso Proyecto de Ingeniería de Software 3, equipo Coffee&Commits).

## Stack

- **Java 21** + **Spring Boot 3.5**
- **Maven** (con wrapper `mvnw`)
- **PostgreSQL 16** + **Flyway** (migraciones)
- **Spring Security** + **OAuth2** (login con Google)
- **Docker** / docker-compose
- WebSocket (notificaciones en tiempo real)

## Arquitectura

Arquitectura en capas, organizada **por módulos que espejan las épicas** del proyecto
(cumple RNF-09, separación de responsabilidades). Cada módulo contiene sus propios
`controller` / `service` / `repository` / `domain` (entidades) / `dto`.

```
cr.brakket
├── config/           Seguridad, CORS, propiedades de Twitch/IA
├── common/           Auditoría, excepciones, respuestas base
├── auth/             EPIC-01  Autenticación y perfiles
├── equipos/          EPIC-02/03  Equipos y plantillas
├── ligas/            EPIC-07  Ligas y temporadas
├── juegos/           EPIC-06  Catálogo de juegos
├── torneos/          EPIC-08  Torneos, fixtures, brackets, inscripciones
├── resultados/       EPIC-09  Resultados y disputas
├── twitch/           EPIC-10  Integración Twitch (captura)
├── sentimiento/      EPIC-10  Análisis de sentimiento (IA)
├── patrocinios/      EPIC-11  Patrocinadores y publicidad
├── notificaciones/   EPIC-12  Notificaciones
├── estadisticas/     EPIC-13  Estadísticas, historial y progresión
└── admin/            EPIC-14  Panel administrativo global
```

El **esquema de base de datos** (26 tablas del diccionario de datos) vive en
`src/main/resources/db/migration/` como migraciones Flyway. **Nunca** se modifica una
migración ya aplicada: se crea una nueva `V<n>__descripcion.sql`.

## Requisitos

- JDK 21
- Docker Desktop (para la base de datos)
- (Opcional) Maven; si no, usar el wrapper `./mvnw`

## Arranque rápido (local)

```bash
# 1. Copiar variables de entorno y completar credenciales
cp .env.example .env

# 2a. Todo en Docker (BD + backend)
docker compose up --build

# 2b. …o solo la BD en Docker y el backend con Maven (recomendado para desarrollar)
docker compose up -d db
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080`. Prueba de humo (público):

```bash
curl http://localhost:8080/api/public/ping
# {"status":"ok","app":"brakket-backend"}
```

Al arrancar, Flyway crea automáticamente las 26 tablas y siembra los roles base.

## Configuración (variables de entorno)

Ver `.env.example`. Claves principales: base de datos (`DB_*` / `POSTGRES_*`),
Google OAuth (`GOOGLE_CLIENT_ID/SECRET`), Twitch (`TWITCH_*`) e IA (`AI_*`).
Las integraciones externas están encapsuladas en `config/TwitchProperties` y
`config/AiProperties` (RNF-23).

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

1. En el módulo correspondiente, crear el `Controller`, `Service` y `Dto`.
2. Usar el `Repository` de la entidad (ya generado en `<modulo>/repository`).
3. Si se necesita una tabla o columna nueva, crear una migración Flyway `V<n>__...sql`
   y actualizar la entidad JPA.
4. Validar entradas con `jakarta.validation` y mapear errores con el
   `GlobalExceptionHandler`.
