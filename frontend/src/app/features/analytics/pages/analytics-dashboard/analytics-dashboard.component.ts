import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AnalyticsService } from '../../services/analytics.service';
import {
  ClasificacionSentimiento,
  MAX_MENSAJES_POR_LOTE,
  SentimientoResultado,
  SerieSentimiento
} from '../../../../models/sentiment.model';
import { TwitchService } from '../../../twitch/services/twitch.service';
import { TransmisionTwitch } from '../../../../models/twitch.model';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { EtiquetaPipe } from '../../../../shared/pipes/etiqueta.pipe';

/**
 * Análisis de sentimiento del chat de una transmisión (RF-39).
 *
 * Herramienta de administración: se elige una transmisión con captura abierta,
 * se pegan mensajes del chat y se ve la clasificación resultante junto con la
 * serie histórica. El chat en vivo lo alimenta el muestreo de RF-38 sin pasar
 * por esta pantalla; el termómetro visual de la serie es RF-40.
 */
@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [FormsModule, DatePipe, DecimalPipe, PageHeaderComponent, EmptyStateComponent, EtiquetaPipe],
  templateUrl: './analytics-dashboard.component.html',
  styleUrl: './analytics-dashboard.component.scss'
})
export class AnalyticsDashboardComponent implements OnInit {
  private readonly analytics = inject(AnalyticsService);
  private readonly twitch = inject(TwitchService);

  readonly maxMensajes = MAX_MENSAJES_POR_LOTE;

  transmisionId: number | null = null;
  mensajesTexto = '';
  usuariosActivos: number | null = null;
  ventanaSegundos: number | null = null;

  readonly transmisiones = signal<TransmisionTwitch[]>([]);
  readonly cargandoTransmisiones = signal(false);
  readonly analizando = signal(false);
  readonly cargandoSerie = signal(false);
  readonly error = signal<string | null>(null);
  readonly resultado = signal<SentimientoResultado | null>(null);
  readonly serie = signal<SerieSentimiento | null>(null);

  ngOnInit(): void {
    this.cargarTransmisiones();
  }

  /**
   * Transmisiones con captura abierta, para elegirlas de una lista en vez de
   * teclear el id: escribir el número a mano solo servía para provocar 404.
   */
  cargarTransmisiones(): void {
    this.cargandoTransmisiones.set(true);
    this.twitch.abiertas().subscribe({
      next: (ts) => {
        this.transmisiones.set(ts);
        // Con una sola transmisión abierta —el caso normal— no tiene sentido
        // obligar a elegirla.
        if (ts.length === 1) {
          this.transmisionId = ts[0].id;
        }
        this.cargandoTransmisiones.set(false);
      },
      error: () => this.cargandoTransmisiones.set(false)
    });
  }

  /** Etiqueta legible de una transmisión en el desplegable. */
  etiqueta(t: TransmisionTwitch): string {
    const contexto = t.torneoId
      ? `torneo ${t.torneoId}`
      : t.partidaId
        ? `partida ${t.partidaId}`
        : 'sin evento asociado';
    return `#${t.id} · ${t.estado} · ${contexto}`;
  }

  analizar(): void {
    this.error.set(null);
    const mensajes = this.mensajesTexto
      .split('\n')
      .map((m) => m.trim())
      .filter((m) => m.length > 0);

    if (!this.transmisionId) {
      this.error.set('Elegí la transmisión que querés analizar.');
      return;
    }
    if (mensajes.length === 0) {
      this.error.set('Pegá al menos un mensaje de chat (uno por línea).');
      return;
    }
    if (mensajes.length > this.maxMensajes) {
      this.error.set(
        `El lote no puede superar los ${this.maxMensajes} mensajes (pegaste ${mensajes.length}).`
      );
      return;
    }

    this.analizando.set(true);
    this.analytics
      .analizar(this.transmisionId, {
        mensajes,
        usuariosActivos: this.usuariosActivos,
        ventanaSegundos: this.ventanaSegundos
      })
      .subscribe({
        next: (r) => {
          this.resultado.set(r);
          this.analizando.set(false);
          this.cargarSerie();
        },
        error: (err) => {
          this.error.set(this.mensajeError(err));
          this.analizando.set(false);
        }
      });
  }

  cargarSerie(): void {
    if (!this.transmisionId) return;
    this.cargandoSerie.set(true);
    this.analytics.serie(this.transmisionId).subscribe({
      next: (s) => {
        this.serie.set(s);
        this.cargandoSerie.set(false);
      },
      error: () => this.cargandoSerie.set(false)
    });
  }

  /** Clase de color según la clasificación, para el badge. */
  tono(clasificacion: ClasificacionSentimiento): string {
    return clasificacion.toLowerCase();
  }

  private mensajeError(err: HttpErrorResponse): string {
    if (err.status === 403) return 'Necesitás rol ADMIN para ejecutar el análisis.';
    if (err.status === 404) return 'Esa transmisión ya no existe. Actualizá la lista.';
    if (err.status === 401) return 'Tu sesión expiró. Iniciá sesión nuevamente.';
    return err.error?.message ?? 'No fue posible completar el análisis.';
  }
}
