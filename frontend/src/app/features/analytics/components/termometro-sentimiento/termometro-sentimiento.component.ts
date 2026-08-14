import { Component, Input, computed, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';

import { ClasificacionSentimiento, Termometro } from '../../../../models/sentiment.model';
import { EtiquetaPipe } from '../../../../shared/pipes/etiqueta.pipe';

/**
 * Termómetro de sentimiento del chat de una transmisión (RF-40).
 *
 * <p>Componente de presentación: no consulta nada, recibe la lectura ya
 * resuelta. Así el mismo indicador puede montarse después en el panel comercial
 * del patrocinador (RF-44) sin arrastrar la carga de datos.</p>
 */
@Component({
  selector: 'app-termometro-sentimiento',
  standalone: true,
  imports: [DecimalPipe, EtiquetaPipe],
  templateUrl: './termometro-sentimiento.component.html',
  styleUrl: './termometro-sentimiento.component.scss'
})
export class TermometroSentimientoComponent {
  private readonly datos = signal<Termometro | null>(null);

  @Input({ required: true })
  set termometro(valor: Termometro | null) {
    this.datos.set(valor);
  }

  readonly lectura = computed(() => this.datos());

  /** Solo con DISPONIBLE se pinta el indicador; la ERS pide ocultarlo si no. */
  readonly hayIndicador = computed(() => {
    const t = this.datos();
    return t !== null && t.estado === 'DISPONIBLE' && t.puntajeGeneral !== null;
  });

  readonly hayEvolucion = computed(() => (this.datos()?.intervalos.length ?? 0) > 1);

  /**
   * Posición de la aguja en la barra, en porcentaje. Convierte el puntaje de
   * [-100, 100] a [0, 100]: 0 es el extremo hostil y 100 el eufórico.
   */
  readonly posicionAguja = computed(() => {
    const puntaje = this.datos()?.puntajeGeneral ?? 0;
    return (puntaje + 100) / 2;
  });

  /** Altura de la barra de un intervalo, en porcentaje del alto disponible. */
  altura(puntaje: number): number {
    // Mínimo 4% para que un intervalo de puntaje 0 siga siendo visible.
    return Math.max(4, Math.abs(puntaje) / 2);
  }

  tono(clasificacion: ClasificacionSentimiento | null): string {
    return (clasificacion ?? 'neutro').toLowerCase();
  }

  /** Etiqueta legible del estado, para el encabezado de la tarjeta. */
  etiquetaEstado(): string {
    switch (this.datos()?.estado) {
      case 'DISPONIBLE':
        return 'Análisis disponible';
      case 'INSUFICIENTE':
        return 'Datos insuficientes';
      case 'PENDIENTE':
        return 'Análisis pendiente';
      default:
        return '';
    }
  }
}
