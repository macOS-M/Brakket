# Guía de arquitectura — Script para presentar la estructura del proyecto

> Para explicar en voz alta cómo está organizado Brakket en una defensa técnica.
> Cada sección tiene **qué es** (para vos) y **qué decir** (para leer/parafrasear).

---

## 0. La idea en 30 segundos

**Qué decir:**
> "Brakket es un monorepo con dos aplicaciones independientes: un **backend** en Spring Boot (Java) que expone una API REST, y un **frontend** en Angular que la consume. Se comunican solo por HTTP con JSON, autenticados con un token JWT. La base de datos es PostgreSQL. Todo se levanta junto con Docker Compose."

**Stack exacto:**
- **Backend**: Java 21, Spring Boot 3.5, JPA/Hibernate + Flyway (migraciones), PostgreSQL 16, seguridad con JWT + login Google (OAuth2).
- **Frontend**: Angular 19 (componentes *standalone* + *signals*), TypeScript, SCSS con tema propio.
- **Orquestación**: Docker Compose (base de datos + backend); el frontend corre con `ng serve`.

**Números** (para dar escala si preguntan): 19 dominios de negocio, 28 controllers, ~52 entidades, 40 repositorios, 36 migraciones de base de datos, 20 features de frontend.

---

## 1. Organización general del repositorio

```
Brakket/
├── backend/         → API REST en Spring Boot (Java)
├── frontend/        → SPA en Angular (TypeScript)
├── scripts/         → utilidades (seed de demo, smoke test de Twitch)
├── docs/            → documentación (esta guía, la guía de demo, el ERS)
├── docker-compose.yml → levanta Postgres + backend
└── .env.example     → plantilla de variables de entorno (el .env real no se sube)
```

**Qué decir:**
> "El repositorio separa claramente backend y frontend. Cada uno se construye y se prueba por su cuenta; el CI de GitHub corre uno u otro según qué carpeta cambió."

---

## 2. BACKEND — organización por dominio

La decisión de diseño clave: **el backend NO se organiza por tipo técnico** (una carpeta gigante de "todos los controllers", otra de "todos los services"). Se organiza **por dominio de negocio**. Cada dominio es una carpeta autocontenida.

```
backend/src/main/java/com/coffeecommits/brakket/
├── auth/          → registro, login, JWT, login con Google
├── tournament/    → torneos, brackets, partidas, resultados (el corazón)
├── team/          → equipos, invitaciones, roles, expulsiones
├── transfer/      → transferencias de jugadores entre equipos
├── league/        → ligas y temporadas
├── game/          → catálogo de juegos + integración con RAWG
├── twitch/        → canal oficial, métricas de audiencia (RF-34/36)
├── transmision/   → página pública de transmisiones (RF-35)
├── sponsorship/   → patrocinadores (RF-41)
├── admin/         → gestión de roles y permisos
├── analytics/ · dispute/ · progression/ · statistics/ · notification/  → módulos de próximas iteraciones
├── upload/        → subida de imágenes (logos, banners)
│
├── common/        → piezas compartidas (respuestas de API, excepciones, utilidades)
├── config/        → configuración transversal (seguridad, CORS, JWT, tareas programadas)
└── BrakketApplication.java → punto de arranque de Spring Boot
```

**Qué decir:**
> "Elegimos organizar por dominio y no por capa técnica. Todo lo de torneos vive junto: su controller, su lógica, sus datos. Así, cuando alguien trabaja en 'equipos', abre una sola carpeta y tiene todo a la vista, sin saltar entre diez carpetas técnicas. Es más fácil de mantener y de repartir el trabajo entre el equipo."

### 2.1 Las 5 capas dentro de cada dominio

Cada dominio (ej. `tournament/`) tiene las mismas cinco subcarpetas. Esto es lo más importante de explicar, porque describe **cómo viaja una petición**:

```
tournament/
├── controller/    → recibe las peticiones HTTP (la "puerta de entrada")
├── dto/           → define la forma de los datos que entran y salen (JSON)
├── service/       → la lógica de negocio y las reglas (el "cerebro")
├── repository/    → habla con la base de datos (el "acceso a datos")
└── model/         → las entidades: cómo se ven las tablas en código Java
```

| Carpeta | Qué hace | Analogía |
|---|---|---|
| **controller** | Expone los endpoints REST (`GET /api/tournaments`, `POST /api/tournaments/{id}/iniciar`). Valida quién puede llamar (`@PreAuthorize`), recibe el request, delega en el service y devuelve la respuesta. **No tiene lógica de negocio.** | El mostrador de recepción |
| **dto** | *Data Transfer Objects*: definen exactamente qué campos entran (`CrearTorneoRequest`) y qué campos salen (`TorneoResponse`). Separan lo que el cliente ve de cómo están guardados los datos por dentro — así la base de datos puede cambiar sin romper la API. | El formulario que llenás |
| **service** | Donde vive **toda la lógica**: validar que la fecha del torneo sea futura, generar el bracket, propagar al ganador, cobrar las reglas de negocio. Es lo único que el controller llama. | El empleado que resuelve tu trámite |
| **repository** | Interfaces que Spring Data implementa solo: `findByJuegoId`, `existsByNombre`. Traducen métodos Java a consultas SQL sin escribir SQL a mano. | El archivo/bodega de datos |
| **model** | Las **entidades**: clases Java anotadas con `@Entity` que representan las tablas (`Torneo`, `Partida`, `Equipo`). Cada campo es una columna. | El molde de cada ficha |

**Qué decir (el recorrido de una petición — decilo señalando):**
> "Cuando el frontend pide crear un torneo, la petición entra por el **controller**, que revisa el permiso y recibe el JSON en un **DTO**. El controller se lo pasa al **service**, que aplica todas las reglas: valida la fecha, arma la estructura. El service usa el **repository** para guardar en la base de datos, y los datos se guardan con la forma que define el **model**. La respuesta vuelve como otro DTO. Este mismo patrón de cinco capas se repite idéntico en los 19 dominios."

### 2.2 Carpetas transversales

- **`config/`** — configuración que aplica a toda la app: `SecurityConfig` (qué rutas son públicas y cuáles piden token), `JwtService` (crear y validar tokens), `CorsConfig` (permitir que el frontend llame), y las *propiedades* de integraciones (Twitch, IA). Es donde se define la seguridad de toda la aplicación.
- **`common/`** — piezas reutilizadas por todos: `ApiResponse` (el envoltorio estándar de las respuestas), `GlobalExceptionHandler` (convierte errores en respuestas HTTP consistentes — un 404, un 409), y utilidades de fecha.

### 2.3 Los recursos (`resources/`)

```
resources/
├── application.yml        → configuración base (puerto, conexión a BD, variables)
├── application-dev.yml    → ajustes de desarrollo (logs detallados)
├── application-prod.yml   → ajustes de producción
└── db/migration/          → 36 migraciones SQL de Flyway (V1, V2, ... V36)
```

**Qué decir:**
> "El esquema de la base de datos **no lo maneja Hibernate**, lo maneja Flyway con migraciones versionadas. Cada cambio de base es un archivo SQL numerado — V1, V2, hasta V36. Cualquiera que levante el proyecto obtiene exactamente la misma base, y el historial de cambios queda registrado. Hibernate solo *valida* que las entidades coincidan con el esquema."

---

## 3. FRONTEND — organización por responsabilidad

El frontend en Angular se divide en cuatro grandes zonas: **core**, **shared**, **layout** y **features**.

```
frontend/src/app/
├── core/          → infraestructura: la plomería que usa toda la app
├── shared/        → piezas de UI reutilizables (botones, tablas, badges…)
├── layout/        → el "marco" de la pantalla (barra lateral, superior, pie)
├── features/      → las 20 pantallas del producto (una carpeta por módulo)
├── models/        → las interfaces TypeScript que espejan los DTOs del backend
├── app.routes.ts  → el mapa de rutas: qué URL carga qué pantalla
└── app.config.ts  → arranque de la app (proveedores, interceptores, idioma)
```

### 3.1 `core/` — la plomería

```
core/
├── services/      → servicios centrales: ApiService (llama al backend),
│                    AuthService (sesión y roles), ThemeService (tema oscuro/claro)
├── guards/        → porteros de rutas: authGuard (exige sesión),
│                    roleGuard (exige un rol, ej. solo ADMIN entra a /admin)
└── interceptors/  → interceptan CADA petición HTTP:
                     jwtInterceptor añade el token; errorInterceptor maneja el 401
```

**Qué decir:**
> "El core es la infraestructura. El `ApiService` centraliza todas las llamadas al backend. Los **guards** son porteros: antes de entrar a una ruta protegida, verifican que haya sesión y el rol correcto. Los **interceptores** son automáticos: cada llamada al backend pasa por ellos, y ahí se le agrega el token de autenticación sin que cada pantalla tenga que preocuparse."

### 3.2 `shared/` — piezas reutilizables

```
shared/
├── components/    → 9 componentes de UI reusables: button, modal, table,
│                    empty-state (estados vacíos/carga), status-badge, stat-card…
├── directives/    → comportamientos aplicables a cualquier elemento (efecto tilt)
├── pipes/         → transformadores de datos en la vista (fecha relativa: "hace 2 h")
├── services/      → servicios de UI compartidos (subida de fotos)
└── utils/         → funciones utilitarias
```

**Qué decir:**
> "Shared son los ladrillos de UI que se repiten en toda la app. En vez de que cada pantalla dibuje su propio 'no hay datos' o su propio badge de estado, todos usan el mismo componente. Eso da consistencia visual y evita repetir código."

### 3.3 `layout/` — el marco

Contiene la barra lateral (el menú de navegación), la barra superior (búsqueda, avatar, sesión) y el pie. Es el "chrome" que envuelve todas las pantallas. Las features se renderizan **dentro** de este marco.

### 3.4 `features/` — las pantallas del producto

Una carpeta por módulo (20 en total): `tournaments`, `teams`, `leagues`, `games`, `transfers`, `transmisiones`, `profile`, `sponsorships`, `admin`, `calendar`, `home` (dashboard)… y las de próxima iteración (`disputes`, `statistics`, `progression`, `notifications`, `analytics`).

**Cada feature tiene la misma estructura interna:**

```
tournaments/
├── pages/         → pantallas completas asociadas a una ruta
│                    (tournament-list = el listado, tournament-detail = el detalle)
├── components/    → piezas propias de esta feature, no reutilizables fuera
│                    (tournament-bracket = el dibujo de la llave,
│                     tournament-wizard = el asistente de creación)
├── services/      → el servicio que llama a los endpoints de este dominio
│                    (tournaments.service.ts → /api/tournaments)
└── tournaments.routes.ts → las sub-rutas de esta feature (carga diferida)
```

| Carpeta | Qué hace |
|---|---|
| **pages** | Pantallas completas, una por URL. `tournament-detail` es todo lo que ves al abrir un torneo. |
| **components** | Piezas que solo tienen sentido dentro de esta feature. El `tournament-bracket` dibuja la llave; no se usa en ningún otro lado. |
| **services** | El puente con el backend: cada método llama a un endpoint (`crear()` → `POST /api/tournaments`). Es lo único de la feature que sabe de HTTP. |
| ***.routes.ts** | Define las rutas hijas y usa **carga diferida** (*lazy loading*): el código de una feature solo se descarga cuando el usuario entra a ella. |

**Qué decir:**
> "Cada feature es un mini-proyecto autocontenido, igual que en el backend. Tiene sus pantallas (**pages**), sus piezas propias (**components**), y un **service** que es el único que habla con el backend. Además, con *lazy loading*, el navegador solo descarga el código de la pantalla que el usuario abre — la app arranca liviana."

### 3.5 `models/` — el contrato con el backend

Interfaces TypeScript que **espejan exactamente los DTOs del backend**. Si el backend devuelve un `TorneoResponse` con ciertos campos, hay un `Torneo` en `models/` con esos mismos campos. Es lo que da seguridad de tipos: si el backend cambia un campo, TypeScript avisa dónde se rompe.

---

## 4. Cómo se conectan backend y frontend (el hilo completo)

**Qué decir (el ejemplo de punta a punta — el momento estrella de la defensa):**
> "Sigamos un clic completo. El usuario abre el detalle de un torneo:
> 1. La **ruta** de Angular (`app.routes.ts`) carga la página `tournament-detail`.
> 2. Esa página llama a su **service** (`tournaments.service.ts`), método `detalle(id)`.
> 3. El service usa el `ApiService`, que dispara un `GET /api/tournaments/{id}`.
> 4. En el camino, el **interceptor** le pega el token JWT a la petición.
> 5. En el backend, `SecurityConfig` deja pasar la ruta, el **controller** `TorneoController` la recibe.
> 6. El controller llama al **service** `TorneoServiceImpl`, que aplica la lógica.
> 7. El service pide los datos al **repository**, que consulta PostgreSQL.
> 8. Los datos (entidades **model**) se empaquetan en un **DTO** `TorneoDetalleResponse` y vuelven como JSON.
> 9. El frontend lo recibe tipado como el `models/tournament.model.ts` y lo pinta en pantalla.
>
> Ese recorrido —controller, DTO, service, repository, model del lado del backend; service, guard, interceptor, page del lado del frontend— es el mismo para cada funcionalidad del sistema."

---

## 5. Los módulos más difíciles (deep dive para preguntas)

Esta sección es para cuando pregunten *"¿qué fue lo más difícil?"* o pidan
detalle de una parte concreta. Cada módulo está contado como **el problema →
cómo lo resolvimos → si preguntan**.

### 5.1 El motor de torneos (generación de brackets)

Es la parte con más lógica del sistema. Soporta **cinco formatos** de
competencia y todos comparten el mismo flujo de reporte de resultados.

**El problema:** un torneo no es solo una lista de partidas. Hay que **generar
la estructura** (la llave) según el formato, **encadenar** cada partida con la
siguiente (el ganador de la partida 1 va a tal lugar de la partida 3),
**propagar** los resultados hacia arriba, y **coronar** al campeón cuando se
cierra la última. Y cinco formatos distintos hacen esto de cinco formas.

**Cómo lo resolvimos — un generador por formato:**
- **Eliminación directa:** se redondea la cantidad de equipos a la potencia de
  2 más cercana (4, 8, 16…) y los que sobran reciben *bye* (pasan de ronda
  gratis). La llave se construye **de la final hacia atrás** para poder enlazar
  cada partida con la siguiente antes de que exista.
- **Doble eliminación (la más difícil):** hay dos llaves, la **superior** y la
  **inferior**, más una **gran final**. Cuando perdés en la superior no quedás
  afuera: caés a la inferior. La dificultad está en calcular a qué partida
  exacta de la llave inferior cae cada perdedor de la superior — es una fórmula
  que depende de la ronda. El que sobrevive la inferior enfrenta al de la
  superior en la gran final.
- **Round robin (todos contra todos):** se genera con el *método del círculo* —
  se fija un equipo y los demás rotan, garantizando que cada par se enfrente
  exactamente una vez.
- **Suizo:** solo se genera la primera ronda; las siguientes se arman **al
  cerrarse la anterior**, emparejando por puntaje y evitando repetir rivales.
- **Fase de grupos + eliminación:** round robin dentro de cada grupo, y los dos
  mejores de cada uno avanzan a una llave cruzada (1° de un grupo vs 2° del
  grupo hermano).

**El flujo de resultados (y por qué es confiable):** una partida no la cierra
una sola persona. El **capitán** de un equipo *reporta* el marcador; el
**capitán rival** lo *confirma* (o lo rechaza, y entonces queda en disputa para
que la resuelva el organizador). Recién ahí el ganador avanza en la llave. Esto
evita que un solo equipo se ponga un resultado a su favor.

**La parte de concurrencia (lo que impresiona):** ¿qué pasa si dos capitanes
tocan la misma partida al mismo tiempo? Usamos **bloqueos pesimistas** en la
base de datos: cuando alguien va a modificar una partida, la fila queda
*bloqueada* hasta que termina, así dos operaciones simultáneas se serializan en
vez de corromperse. Lo mismo al cerrar una etapa: se bloquea el torneo para que
las dos últimas partidas de una jornada no disparen dos veces la generación de
la siguiente ronda.

> **Si preguntan "¿qué fue lo más complejo de programar?"** → "El motor de
> brackets, sobre todo la doble eliminación: calcular a qué partida de la llave
> inferior cae cada perdedor. Y la concurrencia — que dos capitanes reportando a
> la vez no corrompan la llave — la resolvimos con bloqueos pesimistas en la
> base de datos."

### 5.2 Transmisiones e integración con Twitch (RF-35/36)

El reto no fue "mostrar un video", fue **integrarse con Twitch de forma segura
y confiable**, y capturar métricas que solo existen mientras el directo pasa.

**1. El secret nunca llega al navegador.** Twitch da un Client ID y un Client
Secret. Si el frontend pidiera el token directamente, el secret quedaría
expuesto en el navegador de cualquiera. Por eso **el frontend nunca habla con
Twitch**: solo consume nuestro endpoint `GET /api/transmisiones`. Es el backend
el que tiene el secret (en variables de entorno), pide el token y consulta la
API. Además, al pedir el token, el secret viaja en el *cuerpo* del formulario,
no en la URL (las URLs quedan en logs y ahí se filtraría).

**2. El token se cachea y se renueva solo.** En vez de pedir un token nuevo en
cada llamada, el backend lo guarda en memoria y lo reutiliza hasta poco antes de
que expire; si Twitch responde "401 - vencido", lo renueva y reintenta una vez,
sin que el usuario note nada.

**3. Datos frescos sin gastar la cuota (el cálculo de RNF-02).** El requisito
pide que lo mostrado no tenga más de 1 minuto de atraso. Solución con números:
el backend **cachea la respuesta 25 segundos** y el frontend **refresca cada
30** → peor caso 55 segundos, justo por debajo del minuto. Consultar un canal
cada 25s está lejísimos del límite de cuota de Twitch.

**4. Cuando Twitch se cae, la página no se rompe (degradación).** Guardamos
aparte la **última respuesta buena**. Si Twitch no responde, en vez de mostrar
un error, la página muestra esa última información marcada como *"estado
desconocido — última info de hace X min"*. Nunca muestra un dato viejo como si
fuera actual. Cumple el RNF-15 de tolerancia a fallos.

**5. La abstracción para el futuro.** Toda la integración está detrás de una
interfaz `StreamProvider`. La página no sabe que es Twitch. Sumar YouTube o Kick
después es escribir otra implementación de esa interfaz, sin tocar la UI (un
*feature flag* mantiene hoy la lista con un solo canal).

**6. El reproductor embebido.** Twitch exige un parámetro `parent` con el
dominio donde se embebe (medida antifraude). Lo armamos dinámicamente con el
dominio actual, así funciona igual en `localhost` durante el desarrollo y en el
dominio real en producción.

**7. RF-36 — capturar métricas mientras nadie mira.** La audiencia (cuántos
espectadores) solo existe en el presente: Twitch no da histórico. Si no la
guardás en el momento, se pierde. La solución es una **tarea programada** que
corre **cada 60 segundos automáticamente**, guarda una muestra de espectadores
y cierra el período cuando el directo termina. Dos detalles de madurez: si
Twitch falla en un ciclo **no inventa datos** (deja el hueco), y un solo ciclo
sin señal no cierra la transmisión (un streamer que reconecta desaparece un
instante) — se cierra recién al segundo ciclo seguido sin señal.

> **Si preguntan "¿cómo aseguran calidad?"** → contá la historia real: en el
> code review previo al merge encontramos un bug de zona horaria — Twitch
> entrega fechas en UTC y el servidor corre en hora de Costa Rica (UTC-6); sin
> convertir, el panel mostraba "duración: -355 minutos" con el canal en vivo. Lo
> corregimos y agregamos tests antes de que llegara a la demo.

---

## 6. Guion corto para la presentación (2-3 minutos)

1. **Abrí con el panorama** (sección 0): monorepo, backend Spring Boot + frontend Angular, hablan por REST con JWT, Postgres, Docker.
2. **Mostrá el árbol del backend** (2.0) y explicá la decisión: *organizado por dominio, no por capa técnica*.
3. **Explicá las 5 capas** (2.1) con el recorrido de una petición — esto demuestra que entendés el flujo, no solo las carpetas.
4. **Saltá al frontend** (3.0): las cuatro zonas (core, shared, layout, features).
5. **Mostrá una feature** (3.4) y remarcá que replica la misma idea de separación que el backend.
6. **Cerrá con el hilo completo** (sección 4): seguí un clic de punta a punta. Es lo que amarra todo y lo que más impresiona en una defensa.

**Frase de cierre sugerida:**
> "La estructura es repetitiva a propósito: una vez que entendés cómo está armado un dominio, entendés los diecinueve. Esa consistencia es lo que nos permitió repartir el trabajo entre cinco personas sin pisarnos."

---

## 7. Preguntas técnicas probables y respuestas cortas

- **"¿Por qué DTOs en vez de devolver las entidades directamente?"** → Para no acoplar la API a la base de datos, no exponer campos sensibles, y poder cambiar el esquema sin romper el cliente.
- **"¿Dónde está la lógica de negocio?"** → Siempre en la capa **service**. El controller solo recibe y delega; el repository solo lee/escribe.
- **"¿Cómo manejan la seguridad?"** → JWT: el usuario se loguea (local o Google), el backend emite un token, el frontend lo guarda y el interceptor lo manda en cada petición. `SecurityConfig` define rutas públicas vs. protegidas, y `@PreAuthorize` protege acciones por rol.
- **"¿Cómo evolucionan la base de datos?"** → Migraciones Flyway versionadas (V1…V36). Nunca se edita una migración ya aplicada; cada cambio es una nueva.
- **"¿Por qué signals en Angular?"** → Es el modelo de reactividad moderno de Angular 19: el estado se declara como señal y la vista se actualiza sola cuando cambia, sin librerías externas de estado.
- **"¿Qué pasa si el backend se cae o una integración externa falla?"** → Hay degradación: por ejemplo, transmisiones muestra "estado desconocido" en vez de romperse, y los errores se traducen a respuestas HTTP limpias en el `GlobalExceptionHandler`.
