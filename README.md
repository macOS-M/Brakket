# Brakket

Plataforma web para la **gestión y transmisión de ligas y torneos de esports**.
Proyecto del curso *Proyecto de Ingeniería de Software 3* — equipo **Coffee&Commits**.

Brakket cubre el ciclo completo de una competencia: crear ligas y temporadas,
armar torneos con distintos formatos, generar el bracket, reportar y confirmar
resultados, resolver disputas, y llevar estadísticas e historial — sumando
transmisión en vivo por Twitch con métricas de audiencia, análisis de sentimiento
del chat con IA, reportes exportables y patrocinios.

Monorepo con el backend y el frontend en un solo repositorio.

```
brakket/
├── backend/            API REST — Spring Boot 3.5 / Java 21 / PostgreSQL / Flyway
├── frontend/           SPA — Angular 19 (standalone)
├── docs/               Documentación (plan de trabajo, guía de demo E2E)
├── scripts/            Seed de datos de demo
├── .github/workflows/  CI separada (backend / frontend)
├── docker-compose.yml  PostgreSQL + backend
└── .env.example        Variables de entorno (copiar a .env)
```

---

## Stack

| Capa | Tecnología |
|------|-----------|
| Frontend | Angular 19 (standalone, signals) |
| Backend | Java 21 + Spring Boot 3.5 (Maven) |
| Base de datos | PostgreSQL 16 + Flyway (migraciones versionadas) |
| Autenticación | Login local (BCrypt) y Google OAuth 2.0 → JWT propio |
| Integraciones | Twitch (Helix) · IGDB (catálogo de juegos) · Google Gemini (IA) |
| Despliegue | Docker / docker-compose |

---

## Funcionalidades

### 🔐 Autenticación y roles
- **Login local** (correo + contraseña, con freno de fuerza bruta) y **login con Google**.
- Roles y permisos: Administrador, Comisionado, Árbitro, Capitán, Jugador, Patrocinador.
- Perfil de usuario, perfil público de equipo e historial competitivo por juego.

### 🏆 Ligas, temporadas y torneos
- Ligas y temporadas como jerarquía organizativa por juego.
- Torneos con **cinco formatos**: eliminación directa, doble eliminación, round robin, sistema suizo y fase de grupos + eliminación.
- El torneo hereda el **formato y el tope de cupo** de su temporada.
- Inscripción de equipos con validación de tamaño, cupo y fechas.

### 📊 Motor de brackets
- Generación automática de la llave, con manejo de *byes* y potencias de 2.
- Reporte de resultado por el capitán, confirmación del rival y avance automático.
- Coronación del campeón al cerrar la final.

### 👥 Equipos y transferencias
- Registro de equipos multijuego, invitaciones y roles de plantilla.
- Transferencias de jugadores entre equipos con su historial.

### ⚖️ Disputas
- Impugnación de resultados dentro del plazo, con evidencia adjunta.
- Resolución por el organizador o árbitro, con reversión del bracket si corresponde.

### 📺 Streaming y analítica (Twitch)
- Vitrina pública de transmisiones y canal oficial validado.
- **Métricas de audiencia** capturadas del directo (muestras, pico, promedio, duración).
- Consulta de métricas por período, crudas o agregadas por hora.
- **Termómetro de sentimiento del chat** y **asistente de IA** que responde sobre la actividad del chat capturado (con degradación a un analizador léxico determinístico si el proveedor no está disponible).

### 📈 Estadísticas, calendario y progresión
- Estadísticas históricas de equipos y jugadores, segmentadas por juego.
- Calendario de eventos y notificaciones.
- Tienda cosmética / progresión por puntos.

### 💼 Patrocinios y administración
- Patrocinios con cascada liga → torneo y espacios publicitarios por alcance.
- **Reportes** de competencia, audiencia y patrocinio, exportables a PDF.
- Panel de administración de roles y permisos.

---

## Arranque rápido

```bash
git clone <url-del-repo> brakket
cd brakket
cp .env.example .env            # completar credenciales (ver abajo)

# --- Base de datos (Docker) ---
docker compose up -d db

# --- Backend (API en http://localhost:8080) ---
cd backend && ./mvnw spring-boot:run     # Windows: mvnw.cmd spring-boot:run

# --- Frontend (app en http://localhost:4200) ---
cd ../frontend
npm install
npm start
```

Detalles en [`backend/README.md`](backend/README.md) y [`frontend/README.md`](frontend/README.md).

### Variables de entorno imprescindibles

El backend **no arranca** sin estas (fail-fast a propósito):

```env
JWT_SECRET=<al menos 32 caracteres>       # firma de los tokens; sin valor por defecto
DB_URL / DB_USER / DB_PASSWORD            # o los valores por defecto de docker-compose
FRONTEND_URL=http://localhost:4200        # origen permitido para CORS
```

Opcionales según la funcionalidad que se quiera activar:

```env
GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET   # login con Google
TWITCH_CLIENT_ID / TWITCH_CLIENT_SECRET   # canal y métricas (o TWITCH_REQUIRED=false)
TWITCH_CHANNEL=brakketcenfotec
GEMINI_API_KEY                            # asistente de IA (sin ella, cae al analizador léxico)
```

Las credenciales viven **solo** en `.env` (git-ignored); nunca se devuelven por la API
ni se muestran en el frontend.

---

## Datos de demostración

El seed crea, vía la API real, un conjunto de datos consistente (cuentas, ligas,
temporadas, torneos en distintos estados, equipos y patrocinadores):

```bash
node scripts/seed-demo.mjs               # reset + seed (idempotente)
node scripts/seed-demo.mjs --solo-reset  # solo limpiar los datos demo
```

Cuentas de demo (contraseña única `Demo2026!`): `admin.demo@brakket.gg` (ADMIN),
`orga.demo@brakket.gg` (organizadora) y `cap1..cap8.demo@brakket.gg` (capitanes).
El guion completo de la demostración está en [`docs/GUIA-DEMO-E2E.md`](docs/GUIA-DEMO-E2E.md).

---

## Cómo trabajamos

Ramas `feature/RF-XX-...` o `fix/...` desde `develop`; Pull Request a `develop` con
al menos una revisión; **`main` es la rama estable**. La CI corre por separado según
la carpeta modificada (`backend/` o `frontend/`).

Ver [`docs/PLAN-DE-TRABAJO.md`](docs/PLAN-DE-TRABAJO.md) para el flujo Git, la división
por módulos/épicas y la distribución por sprints.

---

## Equipo — Coffee&Commits

Camilo Céspedes · Matías Calvo · Derek Carmiol · Dereck Chavarría · Gabriel Valverde · Marcos Morales

Universidad Cenfotec — Bachillerato en Ingeniería del Software.
