---
name: Brakket
description: Plataforma de administración de ligas y torneos de esports
colors:
  accent: "#ff5500"
  accent-hover: "#ff6a1f"
  accent-text: "#ff6a1f"
  accent-text-light: "#c2410c"
  brand-blue: "#2563eb"
  brand-blue-hover: "#3b82f6"
  brand-cyan: "#22d3ee"
  bg: "#0b1120"
  surface: "#111827"
  surface-raised: "#1f2937"
  surface-sunken: "#0d1526"
  border: "#334155"
  ink: "#e2e8f0"
  ink-muted: "#94a3b8"
  ink-subtle: "#8496ad"
  danger: "#ef4444"
  danger-ink: "#f87171"
  success: "#22c55e"
  success-ink: "#4ade80"
  warning: "#f59e0b"
  warning-ink: "#fbbf24"
  info: "#3b82f6"
  info-ink: "#60a5fa"
  accent-purple: "#8b5cf6"
  accent-purple-ink: "#c4b5fd"
  accent-gold: "#eab308"
  accent-gold-ink: "#facc15"
  on-brand: "#ffffff"
  google-surface: "#ffffff"
  google-ink: "#1f1f1f"
typography:
  display:
    fontFamily: "Barlow, system-ui, sans-serif"
    fontSize: "2rem"
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: "-0.015em"
  headline:
    fontFamily: "Barlow, system-ui, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-0.015em"
  title:
    fontFamily: "Barlow, system-ui, sans-serif"
    fontSize: "1.0625rem"
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: "normal"
  body:
    fontFamily: "Barlow, system-ui, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.55
    letterSpacing: "normal"
  small:
    fontFamily: "Barlow, system-ui, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "normal"
  label:
    fontFamily: "Barlow, system-ui, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: "0.02em"
  micro:
    fontFamily: "Barlow, system-ui, sans-serif"
    fontSize: "0.6875rem"
    fontWeight: 700
    lineHeight: 1.4
    letterSpacing: "0.04em"
  data:
    fontFamily: "Barlow Condensed, Barlow, sans-serif"
    fontSize: "1.75rem"
    fontWeight: 700
    lineHeight: 1.1
    fontFeature: "tabular-nums"
rounded:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  pill: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  xxl: "32px"
components:
  button-primary:
    backgroundColor: "{colors.brand-blue}"
    textColor: "#ffffff"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "7px 14px"
  button-primary-hover:
    backgroundColor: "{colors.brand-blue-hover}"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "7px 14px"
  button-secondary-hover:
    backgroundColor: "{colors.surface-raised}"
  button-danger:
    backgroundColor: "transparent"
    textColor: "{colors.danger-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "7px 14px"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    padding: "16px 18px"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.sm}"
    padding: "9px 12px"
  badge:
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "4px 10px"
  nav-item:
    textColor: "{colors.ink-muted}"
    typography: "{typography.body}"
    rounded: "{rounded.sm}"
    padding: "8px 11px"
  nav-item-active:
    backgroundColor: "{colors.surface-raised}"
    textColor: "{colors.brand-cyan}"
---

# Design System: Brakket

## 1. Overview

**Creative North Star: "La mesa de control de la competencia"**

Brakket es el puesto desde donde se dirige un torneo. Quien lo usa no vino a mirar: vino a aprobar una inscripción, resolver una disputa, cerrar una jornada. Todo lo que importa tiene que estar visible a la vez, y nada puede pedir atención que no se haya ganado. La interfaz es el tablero, no el espectáculo.

De ahí sale la densidad. Este no es un sistema de aire generoso y tarjetas espaciadas; es un sistema que muestra mucho y lo hace legible por jerarquía — peso tipográfico, contraste de tono, alineación de cifras — en vez de por separación. Cuando una pantalla se siente saturada, la respuesta es afilar la jerarquía, no agregar espacio.

La energía competitiva que el producto necesita no viene del color de fondo ni de efectos. Viene de la precisión: números que alinean, estados que se distinguen sin leerse, tipografía con carácter propio. **Brakket usa azul y cyan, la paleta más común del rubro. Eso obliga a que la identidad la carguen la tipografía y la densidad** — si el sistema se apoyara en el color para diferenciarse, sería indistinguible de cualquier dashboard.

Este sistema rechaza explícitamente: la estética de proyecto de curso (cards idénticas repetidas, todo del mismo peso), el gamer llamativo de los 2000 (neones, degradados violeta-azul, texturas), el landing de SaaS (número gigante con etiqueta chiquita, aire donde debería haber información) y el panel de admin sin diseñar (tablas crudas, formularios planos).

**Key Characteristics:**
- Densidad legible por jerarquía, no por espaciado
- Estados distinguibles de un vistazo, nunca solo por color
- Cifras de ancho fijo en todo dato numérico
- Superficies planas en reposo; la sombra es respuesta, no decoración
- Dos temas completos, ambos verificados a WCAG 2.1 AA

## 2. Colors

Paleta fría de trabajo prolongado: azules profundos como base, un cyan que marca lo activo, y una familia de estados que hace todo el trabajo semántico. El acento se reserva para acción y selección; jamás decora.

### Primary
- **Naranja de salida** (`#ff5500`): el color de acción y de identidad — botones primarios, ítem de navegación activo, foco del teclado, selección. Es el naranja de la línea de largada: sobre el azul marino neutro, una sola familia cálida carga toda la energía competitiva. La tinta sobre él es siempre el navy del fondo (`#0b1120`, 5.6:1), nunca blanco (3.1:1, no llega a AA).
- **Naranja hover** (`#ff6a1f`): estado hover del anterior, y también la variante de texto sobre fondo oscuro (5.9:1). En tema claro el texto naranja baja a `#c2410c` (4.9:1).

### Secondary
- **Azul de mando** (`#2563eb`) y **cyan de señal** (`#22d3ee`): degradados a rol informativo y de compatibilidad. Ya no son el color de acción; sobreviven en los estados de información y en pantallas aún no migradas (las que tocan otros PRs).

### Neutral
- **Fondo de sala** (`#0b1120`): el lienzo de la aplicación.
- **Superficie** (`#111827`): tarjetas, paneles, campos de formulario.
- **Superficie elevada** (`#1f2937`): filas dentro de una tarjeta, ítem de navegación activo, hover de superficie.
- **Superficie hundida** (`#0d1526`): la barra lateral, un punto más oscura que el lienzo para que se lea como estructura fija y no como contenido.
- **Borde** (`#334155`): separación explícita entre controles.
- **Tinta** (`#e2e8f0`): texto de cuerpo y títulos.
- **Tinta apagada** (`#94a3b8`): texto secundario, descripciones, etiquetas de campo.
- **Tinta tenue** (`#8496ad`): metadatos, marcas de tiempo, texto de placeholder, secciones aún no disponibles. Es el gris más claro permitido para texto; cualquier valor por debajo incumple AA sobre las superficies del sistema.

### Tertiary
Colores de estado. Cada uno existe en tres variantes: **base** para bordes y acentos sólidos, **-ink** para texto sobre fondo oscuro, y un relleno tenue al 12% de opacidad para chips y banners.

- **Peligro** (`#ef4444` / tinta `#f87171`): destructivo, rechazado, suspendido, disuelto, expulsado.
- **Éxito** (`#22c55e` / tinta `#4ade80`): activo, aceptado, aprobado, confirmado, en curso.
- **Aviso** (`#f59e0b` / tinta `#fbbf24`): pendiente, en revisión, próximo, inscripción abierta.
- **Información** (`#3b82f6` / tinta `#60a5fa`): planificado, programado, borrador.
- **Púrpura de cierre** (`#8b5cf6` / tinta `#c4b5fd`): finalizado. Un estado terminal merece un color que no compita con los activos.
- **Oro de podio** (`#eab308` / tinta `#facc15`): primer puesto, logros, distinciones. Nunca como acento genérico.

### Marca externa
- **Superficie Google** (`#ffffff`) y **tinta Google** (`#1f1f1f`): exclusivos del botón "Continuar con Google". Google exige fondo claro con su logo a color, así que es la única superficie del sistema que rompe el tema oscuro. La familiaridad del control vale más que la coherencia cromática; usarlos en cualquier otro lugar está prohibido.

### Tema claro

El frontmatter es normativo para el tema oscuro, que es el predeterminado. El tema claro reasigna los neutros; los colores de marca y de estado conservan su base y bajan la variante `-ink` para mantener contraste sobre fondo claro.

| Rol | Oscuro | Claro | Contraste en claro |
|---|---|---|---|
| Fondo de sala | `#0b1120` | `#f8fafc` | — |
| Superficie | `#111827` | `#ffffff` | — |
| Superficie elevada | `#1f2937` | `#f1f5f9` | — |
| Superficie hundida | `#0d1526` | `#f1f5f9` | — |
| Borde | `#334155` | `#cbd5e1` | — |
| Tinta | `#e2e8f0` | `#0f172a` | 17.9:1 |
| Tinta apagada | `#94a3b8` | `#475569` | 7.6:1 |
| Tinta tenue | `#8496ad` | `#5a677a` | 5.5:1 |
| **Naranja (texto)** | `#ff6a1f` | `#c2410c` | 4.9:1 |
| **Cyan de señal** (legado) | `#22d3ee` | `#0e7490` | 5.4:1 |
| Peligro (tinta) | `#f87171` | `#dc2626` | 4.8:1 |
| Éxito (tinta) | `#4ade80` | `#15803d` | 5.0:1 |
| Aviso (tinta) | `#fbbf24` | `#b45309` | 5.0:1 |
| Información (tinta) | `#60a5fa` | `#1d4ed8` | 6.7:1 |

**El cyan de marca es inusable como texto sobre blanco** — `#22d3ee` da 1.8:1. Por eso el tema claro lo reemplaza por una variante oscura que conserva el matiz. Es el único color de marca que cambia entre temas, y cambia por obligación, no por gusto.

El tema por defecto es **automático**: sigue a `prefers-color-scheme`, porque el contexto de uso varía por rol — los organizadores trabajan de día y los jugadores de noche. El usuario puede forzar claro u oscuro desde la barra lateral, y esa elección gana sobre la del sistema y se recuerda entre sesiones.

### Named Rules

**La Regla de la Única Acción.** El naranja de salida aparece en una sola acción primaria por pantalla. Un segundo botón naranja convierte a los dos en secundarios.

**La Regla del Color Que No Habla Solo.** Ningún estado se comunica únicamente por color. Todo chip de estado lleva su texto; toda fila con significado lleva además un icono o una etiqueta. Un usuario con daltonismo tiene que poder operar la plataforma completa.

**La Regla del Acento Escaso.** El naranja no supera el 10% de la superficie de una pantalla. Su función es dirigir la mirada; si está en todas partes, no dirige nada. La energía extra viene del contraste y la tipografía, no de más naranja.

## 3. Typography

**Display Font:** Barlow (con `system-ui`, `sans-serif`)
**Body Font:** Barlow — la misma familia, en pesos distintos
**Data Font:** Barlow Condensed (con Barlow, `sans-serif`)

**Character:** Barlow es una grotesca de baja modulación diseñada para señalética deportiva: tiene la firmeza de una fuente de cancha sin la estridencia de una tipografía gamer. Una sola familia carga toda la interfaz — títulos, cuerpo, botones, etiquetas y datos — porque un producto denso con dos familias compitiendo se vuelve ruido. La variante condensada entra solo donde el ancho escasea y las cifras mandan.

### Hierarchy
- **Hero** (800, `2.75rem`, 1.05, mayúsculas): solo portadas (login, futuras landing). Barlow en caja alta lee como señalética de estadio; el acento va en una sola palabra del titular.
- **Display** (700, `2rem`, 1.15, `-0.015em`): título de página. Uno por pantalla.
- **Headline** (700, `1.5rem`, 1.2, `-0.015em`): encabezado de sección mayor.
- **Title** (600, `1.0625rem`, 1.3): título de tarjeta o bloque.
- **Body** (400, `0.875rem`, 1.55): texto general, celdas de tabla, descripciones. Prosa larga a 65–75ch; tablas y datos pueden correr más densos.
- **Small** (400, `0.8125rem`, 1.5): texto de apoyo dentro de una fila densa — fechas, notas al pie de una tarjeta, metadatos.
- **Label** (600, `0.75rem`, `0.02em`): etiquetas de campo, chips de estado, encabezados de columna.
- **Micro** (700, `0.6875rem`, `0.04em`): el paso más chico permitido. Encabezados de grupo en la navegación y chips auxiliares.
- **Data** (Barlow Condensed 700, `1.75rem`, cifras tabulares): valores de métrica, marcadores, posiciones.

La escala es fija en `rem`, no fluida. Los usuarios trabajan a DPI constante y un título que encoge dentro de un panel se ve peor, no mejor.

Ocho pasos parecen muchos frente a los cinco de un sistema editorial, pero una interfaz densa los necesita: entre una etiqueta de columna y el texto de una celda hay una diferencia real de rol. Lo que no está permitido es inventar un noveno.

### Named Rules

**La Regla del Piso de 11px.** `0.6875rem` es el tamaño más chico del sistema. Nada baja de ahí — ni un chip, ni una marca de tiempo, ni una nota al pie. Por debajo de 11px el texto deja de ser leíble para buena parte de los usuarios, y "se ve más prolijo" no es una razón.

### Named Rules

**La Regla de la Cifra Alineada.** Todo número que puede aparecer en columna lleva `font-variant-numeric: tabular-nums`. Marcadores, puntos, porcentajes, contadores. Una tabla de posiciones cuyas cifras bailan entre filas se lee como amateur antes de que nadie sepa por qué.

**La Regla de la Familia Única.** Barlow carga todo. Introducir una segunda familia para "dar personalidad" está prohibido: la personalidad sale del peso, el tamaño y el espaciado.

**Prueba de auditoría:** si un título necesita más de 700 de peso o menos de `-0.04em` de tracking para verse contundente, el problema es la jerarquía de la pantalla, no la tipografía.

## 4. Elevation

Sistema plano en reposo. La profundidad se construye con capas de tono — hundida, lienzo, superficie, elevada — y bordes sutiles, no con sombras permanentes. En una interfaz densa las sombras en reposo ensucian: multiplicadas por veinte tarjetas producen suciedad visual y envejecen mal.

La sombra existe, pero es **respuesta a una acción**, no propiedad de un objeto. Aparece cuando algo se levanta de verdad del plano: un modal sobre su fondo, un menú desplegado. La elevación comunica estado.

### Shadow Vocabulary
- **Reposo** (`box-shadow: none`): el estado por defecto de toda superficie.
- **Tarjeta** (`box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3)`): separación mínima cuando una superficie se apoya sobre otra del mismo tono. Excepcional.
- **Modal** (`box-shadow: 0 24px 80px rgba(0, 0, 0, 0.55)`): diálogos y superficies que flotan sobre un fondo oscurecido. Es la única sombra grande del sistema.

### Named Rules

**La Regla del Plano en Reposo.** Ninguna superficie nace con sombra. Si necesitás una sombra para que una tarjeta se separe del fondo, el contraste de tono entre ambos es insuficiente: arreglá el tono.

**Prueba de auditoría:** si se parece a una app de 2014, la sombra es demasiado oscura y el desenfoque demasiado chico.

## 5. Components

### Buttons
- **Shape:** esquinas suavemente redondeadas (`8px`).
- **Primary:** relleno azul de mando (`#2563eb`) con texto blanco, `7px 14px`, peso 600, `0.8rem`. Hover a azul claro (`#3b82f6`). Una sola por pantalla.
- **Secondary:** fondo transparente con borde de `1px` en borde (`#334155`) y texto tinta. Hover rellena con superficie elevada. Es el botón por defecto: la mayoría de las acciones son secundarias.
- **Danger:** transparente con borde y texto en tinta de peligro (`#f87171`); hover rellena con el tinte al 12%. El relleno sólido rojo se reserva para la confirmación final de un destructivo, nunca para el disparador.
- **Estados obligatorios:** default, hover, focus-visible (anillo cyan de `2px` con `2px` de separación), disabled (`opacity: 0.4`, cursor por defecto) y, en acciones asíncronas, un texto de progreso que reemplaza la etiqueta ("Guardando…").

### Chips
- **Style:** píldora (`999px`), `4px 10px`, etiqueta de `0.72rem` peso 700, relleno del color de estado al 12% con borde del mismo tono al 28% y texto en la variante `-ink`.
- **State:** los chips de filtro invierten a relleno azul sólido con texto blanco cuando están activos; los chips de estado no son interactivos y siempre llevan texto legible, nunca solo color.

### Cards / Containers
- **Corner Style:** `12px`.
- **Background:** superficie (`#111827`) sobre el lienzo.
- **Shadow Strategy:** ninguna en reposo (ver Elevation).
- **Border:** `1px` en borde suave (`rgba(148,163,184,0.18)`); pasa a borde pleno en hover cuando la tarjeta es navegable.
- **Internal Padding:** `16px 18px`.
- **Prohibido:** tarjeta dentro de tarjeta. Si el contenido necesita subdivisión, usá filas sobre superficie elevada.

### Inputs / Fields
- **Style:** fondo de superficie, borde de `1px`, radio `8px`, `9px 12px`, texto de cuerpo. Los campos de búsqueda llevan una lupa de `16px` en tinta tenue a la izquierda, con el relleno izquierdo aumentado a `34px`.
- **Focus:** el borde pasa a cyan de señal; el anillo global de foco cubre la accesibilidad.
- **Placeholder:** tinta tenue. No es una elección estética: la tinta tenue está calibrada para superar 4.5:1 sobre las superficies del sistema, y por eso puede usarse en texto. Bajarla "por elegancia" rompe AA.
- **Error:** borde y mensaje en tinta de peligro, con el mensaje debajo del campo y asociado por `aria-describedby`.

### Navigation
- **Style:** barra lateral fija de `232px` sobre superficie hundida, agrupada por área con encabezados de `0.68rem` en versalitas y tinta tenue.
- **Estados:** reposo en tinta apagada; hover rellena con superficie elevada y sube a tinta; el activo es una **píldora rellena con el acento** y tinta navy (referencia Aquament). La píldora reemplazó a la barra izquierda de 3px.
- **Mobile:** por debajo de `900px` la barra sale del flujo y se despliega desde la izquierda sobre un fondo oscurecido, con un botón hamburguesa fijo.
- **Secciones no disponibles:** llevan un chip "pronto" en tinta tenue. Una sección que no funciona se marca; no se esconde ni se finge.

### Portada determinística *(componente distintivo)*
Las tarjetas de juegos, ligas y torneos llevan una portada de arte generado: un gradiente diagonal cuyo matiz se deriva del nombre de la entidad (`shared/utils/cover.ts`), con el monograma en Barlow Condensed gigante encima y un chip contextual. Mismo nombre → misma portada, siempre. Resuelve dos cosas a la vez: las grillas dejan de ser tarjetas idénticas, y no se embebe arte de juegos con copyright ni imágenes inventadas. Cuando la entidad tenga imagen real cargada, la imagen reemplaza al gradiente.

### Banner promocional *(componente distintivo)*
Franja de acento pleno con un número real en Barlow Condensed grande y su contexto ("N ligas te esperan"). Solo con datos que existen: si el conteo es cero, el banner no se muestra — el estado vacío ya cumple ese rol.

### Stat card *(componente distintivo)*
Tarjeta de métrica con una franja de `2px` del color de su categoría en el borde superior. La cifra va en Barlow Condensed a `1.75rem` con cifras tabulares y toma el color de la franja; la etiqueta va arriba en tinta apagada y la nota debajo en tinta tenue. Es el único lugar del sistema donde un número domina visualmente, y solo porque la métrica es el contenido.

### Empty state *(componente distintivo)*
Un solo componente cubre carga, vacío y error. Carga muestra un anillo giratorio; vacío muestra título, explicación y la acción que resuelve el vacío; error hereda el color de peligro y ofrece reintentar. **Un estado vacío enseña la interfaz**: dice qué falta y cómo llenarlo, nunca "no hay datos".

## 6. Do's and Don'ts

### Do:
- **Do** usar `font-variant-numeric: tabular-nums` en toda cifra que pueda aparecer en columna.
- **Do** acompañar todo estado de color con texto o icono; el color nunca comunica solo.
- **Do** limitar el azul de mando a una acción primaria por pantalla.
- **Do** resolver la densidad con jerarquía tipográfica antes que con espaciado.
- **Do** verificar 4.5:1 en texto de cuerpo y 3:1 en texto grande, **en los dos temas**.
- **Do** dar a cada componente interactivo sus siete estados: default, hover, focus, active, disabled, loading, error.
- **Do** mantener transiciones entre 150 y 250 ms; el usuario está en una tarea.
- **Do** ofrecer alternativa para `prefers-reduced-motion` en toda animación.
- **Do** marcar con un chip "pronto" las secciones sin backend, en vez de esconderlas o simular datos.

### Don't:
- **Don't** mostrar métricas, contadores o datos que no vengan del backend. Ningún número de relleno, en ninguna pantalla.
- **Don't** repetir cards idénticas con icono, título y texto como respuesta por defecto a una lista — es la firma del **proyecto de curso genérico**.
- **Don't** usar neones, degradados violeta-azul, tipografía angulosa ni texturas: el **gamer llamativo de los 2000** es anti-referencia explícita.
- **Don't** construir pantallas con número gigante y etiqueta chiquita, testimonios o aire vacío donde debería haber información — eso es **landing de SaaS**, y Brakket es un producto.
- **Don't** entregar tablas crudas y formularios sin jerarquía: el **panel de admin sin diseñar** es anti-referencia, aunque funcione.
- **Don't** usar `border-left` o `border-right` mayor a `1px` como franja de acento en tarjetas o alertas.
- **Don't** aplicar degradado sobre texto (`background-clip: text`).
- **Don't** anidar tarjetas dentro de tarjetas, nunca.
- **Don't** poner sombra en superficies en reposo.
- **Don't** introducir una segunda familia tipográfica.
- **Don't** usar `alert()` ni `confirm()` del navegador; toda confirmación es en línea y todo error se renderiza en la página.
- **Don't** poner una versalita con tracking encima de cada sección. Un rótulo nombrado y deliberado es voz; un rótulo en todas las secciones es andamio.
