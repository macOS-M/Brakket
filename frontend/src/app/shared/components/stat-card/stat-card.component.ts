import { Component, Input } from '@angular/core';

export type TonoStat = 'azul' | 'cyan' | 'verde' | 'ambar' | 'rojo' | 'morado';

/**
 * Tarjeta de métrica con la franja de color superior del diseño.
 *
 * Uso:
 *   <app-stat-card etiqueta="Equipos" valor="12" nota="2 nuevos" tono="verde" />
 */
@Component({
  selector: 'app-stat-card',
  standalone: true,
  template: `
    <article class="stat" [class]="'tono-' + tono">
      <p class="etiqueta">{{ etiqueta }}</p>
      <p class="valor">{{ valor }}</p>
      @if (nota) {
      <p class="nota">{{ nota }}</p>
      }
    </article>
  `,
  styleUrl: './stat-card.component.scss'
})
export class StatCardComponent {
  @Input({ required: true }) etiqueta = '';
  @Input({ required: true }) valor: string | number = '';
  @Input() nota?: string;
  @Input() tono: TonoStat = 'azul';
}
