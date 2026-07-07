# Brakket

Plataforma web para la **gestión y transmisión de ligas y torneos de esports**
(proyecto del curso Proyecto de Ingeniería de Software 3 — equipo Coffee&Commits).

Monorepo con el backend y el frontend en un solo repositorio.

```
brakket/
├── backend/            API REST — Spring Boot 3.5 / Java 21 / PostgreSQL / Flyway
├── frontend/           SPA — Angular 19
├── docs/               Documentación (plan de trabajo del equipo)
├── .github/workflows/  CI separada (backend-ci / frontend-ci)
├── docker-compose.yml  PostgreSQL + backend
└── .env.example        Variables de entorno (copiar a .env)
```

## Stack

| Capa | Tecnología |
|------|-----------|
| Frontend | Angular 19 (standalone) |
| Backend | Java 21 + Spring Boot 3.5 (Maven) |
| Base de datos | PostgreSQL 16 + Flyway |
| Auth | Google OAuth 2.0 → JWT propio |
| Externos | Twitch API · servicio de IA (análisis de sentimiento) |
| Despliegue | Docker / docker-compose |

## Arranque rápido

```bash
git clone https://github.com/gabovalmon-3/brakket-1.git brakket
cd brakket
cp .env.example .env            # completar credenciales

# --- Backend (API en http://localhost:8080) ---
docker compose up -d db         # base de datos
cd backend && ./mvnw spring-boot:run   # Windows: mvnw.cmd spring-boot:run

# --- Frontend (app en http://localhost:4200) ---
cd ../frontend
npm install
npm start
```

Detalles en [`backend/README.md`](backend/README.md) y
[`frontend/README.md`](frontend/README.md).

## Cómo trabajamos

Ver [`docs/PLAN-DE-TRABAJO.md`](docs/PLAN-DE-TRABAJO.md): flujo Git, división por
módulos/épicas y distribución por sprints.

Resumen: ramas `feature/RF-XX-...` desde `develop`, Pull Request a `develop` con
al menos una revisión; `main` es la rama estable. Commits con la clave de Jira
(`SCRUM-XX RF-YY descripción`).

## Objetivo (ERS)

Demostrar un torneo de prueba con **≥ 4 equipos**: inscripción → generación de
fixtures → reporte de resultados → tabla/bracket actualizados, con roles y disputas
funcionando.
