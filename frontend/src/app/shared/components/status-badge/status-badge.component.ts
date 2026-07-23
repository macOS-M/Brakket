import { Component, Input, computed, signal } from '@angular/core';

export type TonoBadge = 'neutral' | 'info' | 'exito' | 'aviso' | 'peligro' | 'morado' | 'oro';

/**
 * Badge de estado. Centraliza el mapeo estado → color para que
 * "ACTIVO" se vea igual en toda la app.
 *
 * Uso:
 *   <app-status-badge estado="ACTIVA" />
 *   <app-status-badge estado="Platino" tono="morado" />
 */
@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span class="badge" [class]="'tono-' + tonoFinal()">{{ etiqueta() }}</span>`,
  styleUrl: './status-badge.component.scss'
})
export class StatusBadgeComponent {
  private readonly _estado = signal('');
  private readonly _tono = signal<TonoBadge | undefined>(undefined);

  @Input({ required: true }) set estado(valor: string) {
    this._estado.set(valor ?? '');
  }

  /** Fuerza un tono; si se omite se deduce del estado. */
  @Input() set tono(valor: TonoBadge | undefined) {
    this._tono.set(valor);
  }

  /** Texto a mostrar; por defecto el estado capitalizado. */
  @Input() texto?: string;

  readonly etiqueta = computed(() => {
    if (this.texto) {
      return this.texto;
    }
    const estado = this._estado();
    if (!estado) {
      return '';
    }
    // ACTIVO → Activo, INSCRIPCION_ABIERTA → Inscripcion abierta
    const limpio = estado.replace(/_/g, ' ').toLowerCase();
    return limpio.charAt(0).toUpperCase() + limpio.slice(1);
  });

  readonly tonoFinal = computed<TonoBadge>(() => this._tono() ?? this.deducirTono(this._estado()));

  private deducirTono(estado: string): TonoBadge {
    switch (estado.toUpperCase()) {
      case 'ACTIVO':
      case 'ACTIVA':
      case 'ACEPTADA':
      case 'APROBADA':
      case 'CONFIRMADO':
      case 'EN_CURSO':
        return 'exito';

      case 'PENDIENTE':
      case 'EN_REVISION':
      case 'PROXIMO':
      case 'INSCRIPCION_ABIERTA':
        return 'aviso';

      case 'RECHAZADA':
      case 'CANCELADA':
      case 'CANCELADO':
      case 'DISUELTO':
      case 'SUSPENDIDO':
      case 'EXPULSADO':
        return 'peligro';

      case 'PLANIFICADA':
      case 'PROGRAMADO':
      case 'BORRADOR':
        return 'info';

      case 'FINALIZADA':
      case 'FINALIZADO':
        return 'morado';

      default:
        return 'neutral';
    }
  }
}
