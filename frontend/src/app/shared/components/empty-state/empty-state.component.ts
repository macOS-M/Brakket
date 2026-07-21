import { Component, Input } from '@angular/core';

/**
 * Estado vacío / de carga / de error, unificado.
 *
 * Antes cada pantalla resolvía "Cargando…", "No hay datos" y los errores
 * a su manera; esto los deja iguales en toda la app.
 *
 * Uso:
 *   <app-empty-state modo="cargando" />
 *   <app-empty-state titulo="Sin equipos" mensaje="Creá el primero.">
 *     <button>+ Crear equipo</button>
 *   </app-empty-state>
 */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <div class="empty" [class.es-error]="modo === 'error'" role="status">
      @if (modo === 'cargando') {
      <span class="spinner" aria-hidden="true"></span>
      <p class="mensaje">{{ mensaje || 'Cargando…' }}</p>
      } @else {
      @if (titulo) {
      <p class="titulo">{{ titulo }}</p>
      }
      @if (mensaje) {
      <p class="mensaje">{{ mensaje }}</p>
      }
      <div class="accion">
        <ng-content />
      </div>
      }
    </div>
  `,
  styleUrl: './empty-state.component.scss'
})
export class EmptyStateComponent {
  @Input() modo: 'vacio' | 'cargando' | 'error' = 'vacio';
  @Input() titulo?: string;
  @Input() mensaje?: string;
}
