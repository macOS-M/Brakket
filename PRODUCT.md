# Product

## Platform

web

## Users

El usuario primario es el **comisionado u organizador de liga**: quien crea las competencias, configura temporadas y formatos, aprueba inscripciones y transferencias, asigna árbitros y resuelve disputas. Trabaja en sesiones largas sobre pantalla grande, moviéndose entre formularios densos, tablas y colas de trabajo pendiente. Cuando algo se traba, el torneo entero se traba: su tiempo de resolución es el cuello de botella de la plataforma.

El sistema también atiende a capitanes, jugadores y árbitros, cada uno con permisos y flujos propios. Sus pantallas importan, pero cuando una decisión de diseño beneficia a un rol y perjudica al comisionado, gana el comisionado.

El contexto de uso varía según el rol: los organizadores trabajan de día como quien usa una herramienta administrativa; los jugadores entran de noche, alrededor de sus partidas.

## Product Purpose

Brakket administra el ciclo completo de una liga o torneo de esports: creación de ligas y temporadas, inscripción de equipos, generación de fixtures y brackets, reporte de resultados, disputas con evidencia y arbitraje, transferencias de jugadores, estadísticas históricas y progresión competitiva.

Hoy los organizadores arman todo esto con herramientas sueltas — planillas, Discord, mensajes privados — y el registro de lo que pasó vive disperso o directamente se pierde.

Para la iteración actual, el éxito es presentar una plataforma que no parezca un proyecto de curso.

## Positioning

Brakket es el único lugar donde una liga vive completa y auditable de principio a fin — y donde el organizador además puede monetizarla, vendiendo espacios de patrocinio y publicidad dentro de sus propios torneos. Las alternativas resuelven el bracket; ninguna resuelve el ciclo completo y ninguna le da al organizador una forma de sostener económicamente su competencia.

## Brand Personality

Competitiva, enérgica, con actitud. Brakket no es un software administrativo neutro: es la plataforma donde se define quién gana, y eso tiene que sentirse.

La referencia ancla es **FACEIT**, porque resuelve exactamente la tensión de este producto: es densa, competitiva y con identidad fuerte, sin dejar de ser una herramienta seria de trabajo. Challengermode y Toornament aportan el vocabulario del vecindario competitivo; ESPN, FotMob y Understat resuelven bien tablas, calendarios y resultados legibles; Linear, Height y Vercel marcan el estándar de producto denso y pulido.

La actitud se transmite por jerarquía, contraste y precisión — no bajando la calidad de la herramienta.

## Anti-references

- **Proyecto de curso genérico**: Bootstrap por defecto, cards idénticas repetidas, todo gris azulado, espaciado uniforme sin jerarquía.
- **Gamer llamativo de los 2000**: neones, degradados violeta a azul, tipografía angulosa, texturas de fibra de carbono. Estridente sin ser funcional.
- **Landing de SaaS**: número gigante con etiqueta chiquita, testimonios, degradados suaves, aire vacío donde debería haber información.
- **Panel de admin sin diseñar**: tablas crudas, formularios sin jerarquía, todo con el mismo peso visual.

## Design Principles

**Densidad sin ruido.** El comisionado necesita ver mucho a la vez. La respuesta no es agregar aire hasta que respire, sino construir jerarquía para que la densidad se lea. FACEIT y Linear demuestran que densidad y oficio conviven.

**La actitud vive en el detalle, no en el fondo.** La energía competitiva se transmite con tipografía, contraste, color de estado y movimiento con intención. Cualquier intento de lograrla decorando el fondo cae en la anti-referencia gamer.

**Cada estado se lee de un vistazo.** El producto es una máquina de estados: activo, pendiente, en disputa, finalizado, suspendido. Distinguirlos sin leer texto es la tarea de diseño más repetida de la plataforma.

**Mostrar solo lo que existe.** Ninguna métrica inventada, ningún dato de relleno, ningún contador que no venga del backend. Un número falso en una defensa cuesta más que un espacio vacío bien resuelto.

**El patrocinio es ciudadano de primera.** Los espacios de marca son parte del producto y una de sus razones de existir, no un agregado. Tienen que verse dignos sin competir con la competencia misma.

## Accessibility & Inclusion

WCAG 2.1 AA. Contraste mínimo 4.5:1 en texto de cuerpo y 3:1 en texto grande, navegación completa por teclado, foco visible en todos los controles interactivos, etiquetas asociadas en formularios y alternativa para `prefers-reduced-motion` en cada animación.

El color nunca puede ser el único portador de significado: los estados de la competencia deben distinguirse también por texto o forma, no solo por su color.
