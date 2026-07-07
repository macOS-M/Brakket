# Brakket - Frontend

Frontend web de **Brakket**, plataforma de gestion de ligas y torneos de esports.
Aplicacion construida con **Angular 19** usando componentes *standalone* y rutas
*lazy* por feature.

## Stack

- **Angular 19** (componentes standalone, sin NgModules)
- **TypeScript**
- **RxJS** + Angular Signals
- **SCSS** para estilos
- **HttpClient** con interceptor funcional para la sesion

## Requisitos

- **Node.js 20+ / 22** (LTS recomendado)
- **npm** 10+
- Angular CLI (se usa via `npx` o instalada globalmente)

## Puesta en marcha

```bash
# 1. Instalar dependencias
npm install

# 2. Levantar el servidor de desarrollo (http://localhost:4200)
npm start        # equivale a: ng serve

# 3. Compilar para produccion
npm run build
```

## Conexion con el backend

El backend es una API REST (Spring) con autenticacion **Google OAuth2** que corre
en `http://localhost:8080`.

La URL base de la API se configura por entorno:

- `src/environments/environment.ts` -> desarrollo (`apiUrl: 'http://localhost:8080/api'`)
- `src/environments/environment.prod.ts` -> produccion (cambiar `apiUrl` en el despliegue)

Todas las peticiones se hacen con `withCredentials: true` para mantener la cookie de
sesion generada por el flujo OAuth2. El login redirige a
`http://localhost:8080/oauth2/authorization/google` y el logout a
`http://localhost:8080/logout`.

## Estructura de carpetas

```
src/
  environments/            # Configuracion por entorno (apiUrl)
  app/
    core/                  # Servicios singleton, guards, interceptores, modelos
      services/            #   api.service.ts, auth.service.ts
      guards/              #   auth.guard.ts
      interceptors/        #   auth.interceptor.ts
      models/              #   usuario.model.ts
    shared/                # Componentes reutilizables (UI comun)
      components/layout/   #   Barra de navegacion + <router-outlet>
    features/              # Una carpeta por epica / feature
      auth/                #   EPIC-01 login
      perfil/              #   EPIC-01
      equipos/             #   EPIC-02 / EPIC-03
      juegos/              #   EPIC-06
      ligas/               #   EPIC-07
      torneos/             #   EPIC-08 (bracket)
      disputas/            #   EPIC-09
      twitch/              #   EPIC-10 (metricas + termometro)
      patrocinios/         #   EPIC-11
      estadisticas/        #   EPIC-13
      admin/               #   EPIC-14
      home/                #   Landing / dashboard
    app.routes.ts          # Rutas lazy (loadComponent)
    app.config.ts          # Providers (router, http + interceptor)
    app.component.ts       # Componente raiz
```

### Convencion de rutas

- `/login` es una ruta suelta (sin layout).
- El resto de features son hijas de `LayoutComponent` (barra de navegacion).
- `home` y `juegos` son publicas; el resto estan protegidas con `authGuard`.

## Flujo de trabajo Git

- Rama base de integracion: **`develop`**.
- Cada tarea se desarrolla en una rama **`feature/RF-XX`** creada desde `develop`.
- Al terminar se abre un **Pull Request hacia `develop`**.
- Los commits llevan la **clave SCRUM** correspondiente (ej. `SCRUM-123: agregar formulario de equipo`).

```bash
git checkout develop
git pull
git checkout -b feature/RF-XX
# ...trabajo...
git commit -m "SCRUM-123: descripcion del cambio"
git push -u origin feature/RF-XX
# Abrir PR hacia develop
```
