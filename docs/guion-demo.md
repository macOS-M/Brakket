# Guion de demo — viernes (modelo abierto)

Preparación previa (una sola vez, antes de presentar):

1. `docker compose up -d` con `.env` completo (Google OAuth + `RAWG_API_KEY`).
2. Al arrancar, el catálogo se siembra solo con los títulos populares de
   RAWG (pared de juegos con arte). Verificar que `/games` se vea llena.
3. Tener dos cuentas listas en el navegador: una cuenta del equipo
   (`@ucenfotec.ac.cr`, es ADMIN automáticamente al iniciar sesión) y una
   cuenta común de prueba (cualquier Google → entra como JUGADOR).

## Acto 1 — Visitante sin sesión (2 min)

- Entrar sin login: panel público, catálogo de juegos con marquesinas,
  hub de un juego (tab Torneos), ligas y equipos navegables.
- Mensaje: "todo lo de lectura es público; el login se pide al actuar".

## Acto 2 — Usuario común organiza (4 min)

1. "Iniciar sesión" → Google muestra el selector de cuentas → entrar con
   la cuenta común (JUGADor).
2. Buscar un juego en el catálogo (si falta, se agrega solo desde la API
   con "+ Agregar").
3. En el hub del juego: botón **Crear → Torneo** → wizard de 3 pasos
   (General → Equipos → Fecha) → crear torneo público comunitario.
4. Mostrar el torneo en el tab Torneos del juego y en `/tournaments`.
5. (Opcional) Crear → Liga desde el mismo menú: foto propia o arte del
   juego por defecto, descripción y reglas; agregar una temporada.

## Acto 3 — Capitán inscribe a su equipo (3 min)

1. Con la misma cuenta: crear equipo (queda como capitán) e invitar a un
   compañero, o usar un equipo ya armado con plantilla suficiente.
2. Entrar al detalle del torneo → "Inscribí a tu equipo" → elegir equipo
   → inscribir. Ver el equipo y su plantilla en el tab Equipos.
3. Mostrar las validaciones si hay tiempo: cupo, tamaño de plantilla,
   equipo de otra disciplina (mensajes claros del backend).

## Acto 4 — Administración y moderación (3 min)

1. Cambiar de cuenta a la `@ucenfotec.ac.cr` (ADMIN).
2. Mostrar el hub del juego como ADMIN: Editar / Perfil competitivo /
   Desactivar (curaduría del catálogo, RF-20/RF-21).
3. Configurar el perfil competitivo de un juego (p. ej. solo 5v5) y
   mostrar que el wizard ahora acota los tamaños.
4. Moderación: eliminar un torneo o una liga ajena (zona de peligro).
5. `/admin`: asignación de roles (RF-19) — los roles globales siguen
   gobernando la administración de la plataforma.

## Cierre (1 min)

- Decisión de diseño: "modelo abierto de organizadores inspirado en
  Challenger Mode; la habilitación por admin queda como configuración
  futura" (docs/decisiones-diseno.md).
- Deuda declarada: brackets (RF-26/27), invitaciones a torneos privados,
  aprobación de inscripciones.
