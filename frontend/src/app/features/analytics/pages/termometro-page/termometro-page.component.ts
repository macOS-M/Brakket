import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AnalyticsService } from '../../services/analytics.service';
import { Termometro, TransmisionAnalizada } from '../../../../models/sentiment.model';
import { TermometroSentimientoComponent } from '../../components/termometro-sentimiento/termometro-sentimiento.component';
import { AsistenteIaComponent } from '../../components/asistente-ia/asistente-ia.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';

/** Períodos ofrecidos en el filtro; null = toda la transmisión. */
interface OpcionPeriodo {
  etiqueta: string;
  horas: number | null;
  intervaloMinutos: number;
}

/**
 * Termómetro de sentimiento de una transmisión (RF-40).
 *
 * <p>La consultan administradores, comisionados y patrocinadores: todos los que
 * tienen VER_METRICAS_AUDIENCIA. Va en su propia página y no dentro del panel de
 * análisis de RF-39 porque aquel es una herramienta de administración que pega
 * mensajes a mano, y un patrocinador no debería ver ese formulario.</p>
 */
@Component({
  selector: 'app-termometro-page',
  standalone: true,
  imports: [
    FormsModule,
    TermometroSentimientoComponent,
    AsistenteIaComponent,
    PageHeaderComponent,
    EmptyStateComponent
  ],
  templateUrl: './termometro-page.component.html',
  styleUrl: './termometro-page.component.scss'
})
export class TermometroPageComponent implements OnInit {
  private readonly analytics = inject(AnalyticsService);

  /**
   * El intervalo acompaña al período: media hora con tramos de una hora daría
   * una sola columna, y una transmisión entera con tramos de un minuto daría
   * cientos y el backend la rechazaría.
   */
  readonly periodos: OpcionPeriodo[] = [
    { etiqueta: 'Última hora', horas: 1, intervaloMinutos: 5 },
    { etiqueta: 'Últimas 6 horas', horas: 6, intervaloMinutos: 15 },
    { etiqueta: 'Últimas 24 horas', horas: 24, intervaloMinutos: 60 },
    { etiqueta: 'Toda la transmisión', horas: null, intervaloMinutos: 15 }
  ];

  transmisionId: number | null = null;
  periodo: OpcionPeriodo = this.periodos[3];

  readonly transmisiones = signal<TransmisionAnalizada[]>([]);
  readonly cargandoTransmisiones = signal(false);
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  readonly termometro = signal<Termometro | null>(null);

  /**
   * Inicio del período de la última consulta. Se guarda en vez de recalcularlo
   * para que el asistente pregunte sobre exactamente la misma ventana que
   * produjo el termómetro visible, y no sobre una que se corre sola con el reloj.
   */
  readonly desdeConsultado = signal<string | null>(null);

  ngOnInit(): void {
    this.cargarTransmisiones();
  }

  cargarTransmisiones(): void {
    this.cargandoTransmisiones.set(true);
    this.analytics.transmisionesAnalizadas().subscribe({
      next: (ts) => {
        this.transmisiones.set(ts);
        this.cargandoTransmisiones.set(false);
        // Con una sola transmisión analizada no tiene sentido hacer elegir.
        if (ts.length === 1) {
          this.transmisionId = ts[0].id;
          this.consultar();
        }
      },
      error: (err) => {
        this.error.set(this.mensajeError(err));
        this.cargandoTransmisiones.set(false);
      }
    });
  }

  /**
   * Etiqueta legible de una transmisión en el desplegable.
   *
   * Se muestra el nombre del torneo y no su id: "torneo 27" no se puede
   * resolver desde ninguna pantalla. El id queda de respaldo por si una
   * transmisión vieja quedó colgando de un torneo sin nombre.
   */
  etiqueta(t: TransmisionAnalizada): string {
    const contexto = t.torneoNombre ?? (t.torneoId ? `torneo ${t.torneoId}` : 'sin torneo');
    return `#${t.id} · ${t.estado} · ${contexto} · ${t.totalMuestras} muestras`;
  }

  consultar(): void {
    this.error.set(null);
    if (!this.transmisionId) {
      this.error.set('Elegí la transmisión que querés consultar.');
      return;
    }

    const desde = this.inicioDelPeriodo();
    this.desdeConsultado.set(desde);

    this.cargando.set(true);
    this.analytics
      .termometro(this.transmisionId, {
        desde,
        intervaloMinutos: this.periodo.intervaloMinutos
      })
      .subscribe({
        next: (t) => {
          this.termometro.set(t);
          this.cargando.set(false);
        },
        error: (err) => {
          this.termometro.set(null);
          this.error.set(this.mensajeError(err));
          this.cargando.set(false);
        }
      });
  }

  /**
   * Inicio del período en hora local, sin zona: el backend recibe
   * LocalDateTime, así que mandar un ISO con Z lo correría por el desfase.
   */
  private inicioDelPeriodo(): string | null {
    if (this.periodo.horas === null) return null;
    const desde = new Date(Date.now() - this.periodo.horas * 3600_000);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${desde.getFullYear()}-${pad(desde.getMonth() + 1)}-${pad(desde.getDate())}`
      + `T${pad(desde.getHours())}:${pad(desde.getMinutes())}:${pad(desde.getSeconds())}`;
  }

  private mensajeError(err: HttpErrorResponse): string {
    if (err.status === 403) return 'No tenés permiso para ver la analítica de audiencia.';
    if (err.status === 404) return 'Esa transmisión ya no existe. Actualizá la lista.';
    if (err.status === 401) return 'Tu sesión expiró. Iniciá sesión nuevamente.';
    return err.error?.message ?? 'No fue posible cargar el termómetro.';
  }
}
