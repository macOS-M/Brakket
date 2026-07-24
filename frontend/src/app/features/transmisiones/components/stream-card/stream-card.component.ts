import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DecimalPipe } from '@angular/common';

import { TarjetaTransmision, Transmision } from '../../../../models/transmision.model';

/**
 * Tarjeta de la grilla de transmisiones, estilo home de Twitch: thumbnail
 * con badge de estado y espectadores, y debajo avatar, título, canal,
 * categoría y tags. La variante "próximamente" rellena los huecos de la
 * grilla desde la misma estructura de datos (nunca markup estático).
 */
@Component({
  selector: 'app-stream-card',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './stream-card.component.html',
  styleUrl: './stream-card.component.scss'
})
export class StreamCardComponent {
  @Input({ required: true }) tarjeta!: TarjetaTransmision;

  /** El thumbnail selecciona la transmisión en el hero destacado. */
  @Output() seleccionada = new EventEmitter<Transmision>();

  get transmision(): Transmision | null {
    return this.tarjeta.tipo === 'real' ? this.tarjeta.transmision : null;
  }

  etiquetaIdioma(codigo: string | null): string | null {
    if (!codigo) {
      return null;
    }
    const nombres: Record<string, string> = { es: 'Español', en: 'Inglés', pt: 'Portugués' };
    return nombres[codigo] ?? codigo.toUpperCase();
  }
}
