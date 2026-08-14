import { Component, EventEmitter, Input, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AnalyticsService } from '../../services/analytics.service';
import { TurnoAsistente } from '../../../../models/sentiment.model';
import { AuthService } from '../../../../core/services/auth.service';

/**
 * Asistente de análisis de la transmisión (RF-40).
 *
 * <p>Botón flotante y panel de conversación sobre la página del termómetro.
 * Responde en lenguaje natural sobre la actividad del chat y el sentimiento,
 * usando las mismas series que ya muestra la vista.</p>
 *
 * <p>Solo se pinta para ADMIN: cada pregunta sale hacia un proveedor externo y
 * consume cuota, así que no se ofrece a comisionados ni patrocinadores. El
 * backend igual lo exige, esto solo evita mostrar un botón que daría 403.</p>
 *
 * <p>El historial es de la vista, no de la conversación: el backend atiende cada
 * pregunta por separado y el modelo no lee los turnos anteriores. Las sugerencias
 * empujan hacia preguntas que se bastan solas.</p>
 */
@Component({
  selector: 'app-asistente-ia',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './asistente-ia.component.html',
  styleUrl: './asistente-ia.component.scss'
})
export class AsistenteIaComponent {
  private readonly analytics = inject(AnalyticsService);
  private readonly auth = inject(AuthService);

  /** Transmisión sobre la que se pregunta; null mientras no se elija ninguna. */
  @Input({ required: true }) transmisionId: number | null = null;

  /** Inicio del período que el usuario tiene seleccionado; null = todo. */
  @Input() desde: string | null = null;

  /**
   * Se emite cuando una clasificación forzada dejó análisis nuevo. La página lo
   * usa para recargar el selector: una transmisión recién empezada no figura
   * hasta tener su primer análisis, y sin esto habría que refrescar a mano
   * justo después de haber pedido el análisis desde acá.
   */
  @Output() analisisHecho = new EventEmitter<void>();

  readonly esAdmin = computed(() => this.auth.hasRole('ADMIN'));

  readonly abierto = signal(false);
  readonly turnos = signal<TurnoAsistente[]>([]);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  /** Clasificación de sentimiento pedida a mano, fuera de la cadencia del bloque. */
  readonly analizando = signal(false);
  readonly avisoAnalisis = signal<string | null>(null);

  pregunta = '';

  /** Arranques que el asistente puede responder con los datos que existen. */
  readonly sugerencias = [
    '¿A qué hora hubo más actividad en el chat?',
    '¿Cuándo bajó la participación?',
    '¿Qué momentos conviene revisar según el sentimiento?'
  ];

  alternar(): void {
    this.abierto.update((v) => !v);
  }

  usarSugerencia(texto: string): void {
    this.pregunta = texto;
    this.enviar();
  }

  enviar(): void {
    const texto = this.pregunta.trim();
    if (!texto || this.enviando()) {
      return;
    }
    if (!this.transmisionId) {
      this.error.set('Elegí una transmisión antes de preguntar.');
      return;
    }

    this.error.set(null);
    this.turnos.update((t) => [...t, { autor: 'usuario', texto }]);
    this.pregunta = '';
    this.enviando.set(true);

    this.analytics.preguntarAsistente(this.transmisionId, texto, { desde: this.desde }).subscribe({
      next: (r) => {
        this.turnos.update((t) => [
          ...t,
          { autor: 'asistente', texto: r.respuesta, generadaPorIa: r.generadaPorIa, aviso: r.aviso }
        ]);
        this.enviando.set(false);
      },
      error: (err) => {
        this.error.set(this.mensajeError(err));
        this.enviando.set(false);
      }
    });
  }

  /**
   * Fuerza la clasificación del chat acumulado. El muestreo la hace sola cada
   * quince minutos; esto sirve cuando hace falta una lectura ya, sin esperar a
   * que se cumpla el bloque.
   */
  analizarAhora(): void {
    if (this.analizando()) {
      return;
    }
    this.error.set(null);
    this.avisoAnalisis.set(null);
    this.analizando.set(true);

    this.analytics.clasificarSentimientoAhora().subscribe({
      next: (r) => {
        this.avisoAnalisis.set(r.mensaje);
        this.analizando.set(false);
        if (r.clasificado) {
          this.analisisHecho.emit();
        }
      },
      error: (err) => {
        this.error.set(this.mensajeError(err));
        this.analizando.set(false);
      }
    });
  }

  limpiar(): void {
    this.turnos.set([]);
    this.error.set(null);
    this.avisoAnalisis.set(null);
  }

  private mensajeError(err: HttpErrorResponse): string {
    if (err.status === 403) return 'Solo un administrador puede usar el asistente.';
    if (err.status === 401) return 'Tu sesión expiró. Iniciá sesión nuevamente.';
    if (err.status === 404) return 'Esa transmisión ya no existe. Actualizá la lista.';
    return err.error?.message ?? 'No fue posible consultar al asistente.';
  }
}
