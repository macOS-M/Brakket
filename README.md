Proyecto# ArenaSync

Plataforma web para la administración integral de ligas y torneos de videojuegos competitivos.

## 📖 Descripción General

ArenaSync es una plataforma diseñada para gestionar competencias esports de forma centralizada, organizada y transparente. El sistema cubre todo el ciclo de vida de una liga o torneo: desde la creación de temporadas y fixtures hasta la resolución de disputas, estadísticas históricas y progresión competitiva de jugadores y equipos.

La plataforma ofrece herramientas especializadas para:

- Organizadores y comisionados
- Árbitros
- Capitanes de equipo
- Jugadores

Cada rol cuenta con funcionalidades y permisos adaptados a sus responsabilidades dentro de la competencia.

---

# 🎯 Objetivo General

Desarrollar una plataforma web completa para la gestión de ligas y torneos esports que permita administrar competencias de forma profesional, escalable y transparente.

---

# 🚀 Funcionalidades Principales

## 🔐 Autenticación y Perfiles
- Inicio de sesión con Google
- Perfil público de jugador
- Perfil de equipo
- Historial competitivo
- Timeline de logros
- Historial de sanciones
- Auditoría de actividad

## 🏆 Gestión de Ligas y Torneos
- Creación de ligas y temporadas
- Configuración de torneos
- Reglas personalizadas
- Inscripción abierta o aprobada
- Configuración de formatos competitivos

### Formatos soportados
- Eliminación simple
- Eliminación doble (llave inferior y gran final)
- Round Robin (tabla de posiciones)
- Sistema Suizo (emparejamiento por marcas)
- Fase de grupos + llave eliminatoria

## 📊 Fixtures y Brackets
- Generación automática de enfrentamientos
- Propagación automática de resultados
- Manejo de walkovers y byes
- Brackets interactivos en tiempo real

## 👥 Gestión de Equipos
- Registro de equipos
- Invitaciones a jugadores
- Transferencias entre equipos
- Restricciones por ventanas de transferencia
- Configuración de roster mínimo y máximo

## ⚖️ Sistema de Disputas
- Reporte de resultados
- Adjuntar evidencia
- Flujo formal de disputas
- Historial auditable
- Resolución por árbitros y comisionados

## 🔔 Notificaciones
- Notificaciones en tiempo real
- Cambios de estado
- Actualizaciones administrativas
- Alertas de sanciones y resoluciones

## 📈 Estadísticas e Historial
- Tabla de posiciones en tiempo real
- Estadísticas por jugador
- Estadísticas por equipo
- Historial longitudinal
- Calendario competitivo

## 🎖️ Sistema de Progresión
- Logros y recompensas
- Puntos de experiencia
- Títulos personalizados
- Insignias y marcos de perfil

## 🛠️ Panel Administrativo
### Comisionados
- Gestión de torneos
- Configuración de reglas
- Supervisión general

### Árbitros
- Gestión de disputas
- Revisión de evidencia
- Emisión de resoluciones

---

# 💡 Propuesta de Valor

La plataforma busca resolver problemas que las soluciones actuales no cubren completamente:

- Gestión de ligas de largo plazo
- Sistema flexible de reglas
- Transparencia administrativa total
- Historial competitivo persistente
- Engagement más allá del torneo

---

# 👥 Público Objetivo

- Organizadores de ligas esports
- Comunidades competitivas
- Equipos y jugadores
- Árbitros y moderadores
- Universidades y asociaciones gaming




---

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

## Configuración de Twitch (RF-34)

El canal oficial configurado para desarrollo es
`https://www.twitch.tv/brakketcenfotec`. Sin credenciales, el panel permite
guardarlo con estado `PENDIENTE`; la validación real se habilita al definir:

```env
TWITCH_CLIENT_ID=...
TWITCH_CLIENT_SECRET=...
TWITCH_CHANNEL=brakketcenfotec
```

Las credenciales se obtienen registrando una aplicación en
https://dev.twitch.tv/console/apps. Para desarrollo local se puede registrar
`http://localhost:8080/api/twitch/oauth/callback` como URL de redirección.
El secreto debe permanecer únicamente en `.env` o en el gestor de secretos del
ambiente de despliegue; nunca se devuelve mediante la API ni se muestra en Angular.

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
