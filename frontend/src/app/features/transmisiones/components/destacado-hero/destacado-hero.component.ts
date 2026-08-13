import { Component, Input, computed, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';

import { Transmision } from '../../../../models/transmision.model';
import { TwitchPlayerComponent } from '../twitch-player/twitch-player.component';
import { FechaRelativaPipe } from '../../../../shared/pipes/fecha-relativa.pipe';
import { AdSlotComponent } from '../../../../shared/components/ad-slot/ad-slot.component';

/**
 * Carrusel destacado de la página de transmisiones, estilo home de Twitch:
 * player reproduciéndose a la izquierda y tarjeta de info del canal al lado.
 *
 * <p>La tercera columna del grid es el hueco reservado para el chat embebido
 * (fuera de alcance por ahora): activarlo será quitar el [hidden] y añadir la
 * clase `con-chat`, sin rehacer la grilla.</p>
 *
 * <p>Accesibilidad: navegable por teclado (flechas y botones), sin rotación
 * automática — el movimiento solo ocurre a pedido del usuario.</p>
 */
@Component({
  selector: 'app-destacado-hero',
  standalone: true,
  imports: [DecimalPipe, TwitchPlayerComponent, FechaRelativaPipe, AdSlotComponent],
  templateUrl: './destacado-hero.component.html',
  styleUrl: './destacado-hero.component.scss'
})
export class DestacadoHeroComponent {
  private readonly _transmisiones = signal<Transmision[]>([]);
  readonly indice = signal(0);

  @Input({ required: true }) set transmisiones(valor: Transmision[]) {
    this._transmisiones.set(valor ?? []);
    if (this.indice() >= (valor?.length ?? 0)) {
      this.indice.set(0);
    }
  }

  readonly lista = computed(() => this._transmisiones());
  readonly actual = computed<Transmision | null>(() => this.lista()[this.indice()] ?? null);
  readonly hayVarias = computed(() => this.lista().length > 1);

  anterior(): void {
    const total = this.lista().length;
    if (total > 1) {
      this.indice.update((i) => (i - 1 + total) % total);
    }
  }

  siguiente(): void {
    const total = this.lista().length;
    if (total > 1) {
      this.indice.update((i) => (i + 1) % total);
    }
  }

  ir(indice: number): void {
    this.indice.set(indice);
  }

  /** Selección desde una tarjeta de la grilla (la página delega aquí). */
  seleccionar(transmision: Transmision): void {
    const posicion = this.lista().findIndex((t) => t.loginCanal === transmision.loginCanal);
    if (posicion >= 0) {
      this.indice.set(posicion);
    }
  }

  manejarTeclado(evento: KeyboardEvent): void {
    if (evento.key === 'ArrowLeft') {
      evento.preventDefault();
      this.anterior();
    } else if (evento.key === 'ArrowRight') {
      evento.preventDefault();
      this.siguiente();
    }
  }
}
