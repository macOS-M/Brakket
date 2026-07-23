import { Component, Input } from '@angular/core';

/**
 * Placeholder digno para secciones cuyo módulo todavía no tiene backend.
 * Es honesto (no simula datos) pero se ve intencional: título, la promesa
 * de la sección y un chip "próximamente", en vez de un "Pendiente EPIC-XX".
 *
 * Uso:
 *   <app-coming-soon
 *     titulo="Torneos"
 *     descripcion="Creá torneos, generá fixtures y brackets, y seguí los resultados en vivo.">
 *   </app-coming-soon>
 */
@Component({
  selector: 'app-coming-soon',
  standalone: true,
  template: `
    <section class="proximamente">
      <div class="tarjeta">
        <span class="chip">Próximamente</span>
        <h1>{{ titulo }}</h1>
        <p class="descripcion">{{ descripcion }}</p>
        <p class="nota">
          Esta sección se habilitará cuando su módulo esté listo. El resto de la
          plataforma ya está operativo.
        </p>
      </div>
    </section>
  `,
  styleUrl: './coming-soon.component.scss'
})
export class ComingSoonComponent {
  @Input({ required: true }) titulo = '';
  @Input({ required: true }) descripcion = '';
}
