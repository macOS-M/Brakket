import { Component, Input } from '@angular/core';

/**
 * Encabezado de página: título, subtítulo y una zona de acciones a la derecha.
 * Reemplaza al <h1> suelto que cada pantalla resolvía a su manera.
 *
 * Uso:
 *   <app-page-header titulo="Equipos" subtitulo="Gestiona tus plantillas">
 *     <button>+ Crear equipo</button>
 *   </app-page-header>
 */
@Component({
  selector: 'app-page-header',
  standalone: true,
  template: `
    <header class="page-header">
      <div class="titulos">
        @if (eyebrow) {
        <span class="eyebrow">{{ eyebrow }}</span>
        }
        <h1>{{ titulo }}</h1>
        @if (subtitulo) {
        <p class="subtitulo">{{ subtitulo }}</p>
        }
      </div>
      <div class="acciones">
        <ng-content />
      </div>
    </header>
  `,
  styleUrl: './page-header.component.scss'
})
export class PageHeaderComponent {
  @Input({ required: true }) titulo = '';
  @Input() subtitulo?: string;
  @Input() eyebrow?: string;
}
