import { Component, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AnalyticsService } from '../../services/analytics.service';
import {
  ClasificacionSentimiento,
  SentimientoResultado,
  SerieSentimiento
} from '../../../../models/sentiment.model';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';

/**
 * Análisis de sentimiento del chat de una transmisión (RF-39).
 *
 * Un admin pega los mensajes capturados del chat, los analiza y ve la
 * clasificación resultante y la serie histórica. El termómetro visual de esa
 * serie es RF-40.
 */
@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [FormsModule, DatePipe, DecimalPipe, PageHeaderComponent, EmptyStateComponent],
  templateUrl: './analytics-dashboard.component.html',
  styleUrl: './analytics-dashboard.component.scss'
})
export class AnalyticsDashboardComponent {
  private readonly analytics = inject(AnalyticsService);

  transmisionId: number | null = null;
  mensajesTexto = '';
  usuariosActivos: number | null = null;

  readonly analizando = signal(false);
  readonly cargandoSerie = signal(false);
  readonly error = signal<string | null>(null);
  readonly resultado = signal<SentimientoResultado | null>(null);
  readonly serie = signal<SerieSentimiento | null>(null);

  analizar(): void {
    this.error.set(null);
    const mensajes = this.mensajesTexto
      .split('\n')
      .map((m) => m.trim())
      .filter((m) => m.length > 0);

    if (!this.transmisionId) {
      this.error.set('Indicá el ID de la transmisión.');
      return;
    }
    if (mensajes.length === 0) {
      this.error.set('Pegá al menos un mensaje de chat (uno por línea).');
      return;
    }

    this.analizando.set(true);
    this.analytics
      .analizar(this.transmisionId, { mensajes, usuariosActivos: this.usuariosActivos })
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
    if (err.status === 404) return 'No existe una transmisión con ese ID.';
    if (err.status === 401) return 'Tu sesión expiró. Iniciá sesión nuevamente.';
    return err.error?.message ?? 'No fue posible completar el análisis.';
  }
}
