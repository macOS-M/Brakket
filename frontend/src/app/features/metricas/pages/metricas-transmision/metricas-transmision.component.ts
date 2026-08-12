import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { FechaInputComponent } from '../../../../shared/components/fecha-input/fecha-input.component';

import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatCardComponent } from '../../../../shared/components/stat-card/stat-card.component';
import {
  GraficoSerieComponent,
  SerieGrafico,
} from '../../components/grafico-serie/grafico-serie.component';
import { aEpoch, cadenciaMs, indiceMasCercano } from '../../components/grafico-serie/escalas';
import { MetricasTransmisionService } from '../../services/metricas-transmision.service';
import {
  AgrupacionMetricas,
  ClaveSerie,
  SerieMetrica,
  SeriesTransmision,
  TransmisionAnalizable,
} from '../../../../models/analitica.model';

interface FilaTooltip {
  etiqueta: string;
  valor: string;
}

/** RF-37: consulta de métricas de una transmisión por período y rango horario. */
@Component({
  selector: 'app-metricas-transmision',
  standalone: true,
  imports: [FormsModule, PageHeaderComponent, StatCardComponent, EmptyStateComponent, GraficoSerieComponent, FechaInputComponent],
  templateUrl: './metricas-transmision.component.html',
  styleUrl: './metricas-transmision.component.scss',
})
export class MetricasTransmisionComponent implements OnInit {
  private readonly analytics = inject(MetricasTransmisionService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly transmisiones = signal<TransmisionAnalizable[]>([]);
  readonly cargandoCatalogo = signal(true);
  readonly errorCatalogo = signal<string | null>(null);

  // Filtros en borrador: no disparan la consulta hasta "Buscar", como el calendario.
  readonly transmisionFiltro = signal<number | null>(null);
  readonly desdeFiltro = signal('');
  readonly hastaFiltro = signal('');
  readonly agrupacionFiltro = signal<AgrupacionMetricas>('HORA');

  readonly datos = signal<SeriesTransmision | null>(null);
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  readonly indiceActivo = signal<number | null>(null);

  /** Descarta respuestas viejas si el usuario dispara varias consultas seguidas. */
  private token = 0;

  // Constante y no un literal en el template: el input es una señal y compara
  // por referencia, así que un objeto nuevo por ciclo lo redibujaría siempre.
  readonly dominioSentimiento = { min: -1, max: 1 };

  readonly rangoInvalido = computed(
    () => !!this.desdeFiltro() && !!this.hastaFiltro() && this.desdeFiltro() > this.hastaFiltro()
  );

  readonly instantes = computed<number[]>(() => {
    const series = this.datos()?.series ?? [];
    const todos = new Set<number>();
    series.forEach((s) => s.puntos.forEach((p) => todos.add(aEpoch(p.instante))));
    return [...todos].sort((a, b) => a - b);
  });

  readonly graficoAudiencia = computed<SerieGrafico[]>(() =>
    this.aGrafico(['ESPECTADORES'], { ESPECTADORES: 'serie-espectadores' }, true)
  );

  readonly graficoChat = computed<SerieGrafico[]>(() =>
    this.aGrafico(['MENSAJES_POR_MINUTO', 'USUARIOS_ACTIVOS'], {
      MENSAJES_POR_MINUTO: 'serie-mensajes',
      USUARIOS_ACTIVOS: 'serie-usuarios',
    })
  );

  readonly graficoSentimiento = computed<SerieGrafico[]>(() =>
    this.aGrafico(['SENTIMIENTO'], { SENTIMIENTO: 'serie-sentimiento' }, true)
  );

  /** Agrupado por hora, un rango corto colapsa a un punto y el gráfico se ve vacío. */
  readonly unicoBucket = computed(
    () => this.datos()?.agrupacion === 'HORA' && this.instantes().length === 1
  );

  readonly audienciaDisponible = computed(() => this.tieneDatos('ESPECTADORES'));
  readonly chatDisponible = computed(
    () => this.tieneDatos('MENSAJES_POR_MINUTO') || this.tieneDatos('USUARIOS_ACTIVOS')
  );
  readonly sentimientoDisponible = computed(() => this.tieneDatos('SENTIMIENTO'));

  readonly instanteActivo = computed<string | null>(() => {
    const indice = this.indiceActivo();
    const instantes = this.instantes();
    if (indice === null || indice < 0 || indice >= instantes.length) {
      return null;
    }
    return new Date(instantes[indice]).toLocaleTimeString('es-CR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  });

  /**
   * Valores de cada serie en el instante marcado. Con agrupación cruda las series
   * no comparten timestamp, así que se busca la muestra más cercana dentro de una
   * cadencia; más lejos que eso se muestra vacío en vez de mentir.
   */
  readonly filasTooltip = computed<FilaTooltip[]>(() => {
    const indice = this.indiceActivo();
    const instantes = this.instantes();
    const datos = this.datos();
    if (indice === null || !datos || indice < 0 || indice >= instantes.length) {
      return [];
    }
    const objetivo = instantes[indice];
    const tolerancia = cadenciaMs(instantes, datos.intervaloSegundos);

    return datos.series
      .filter((s) => s.puntos.length)
      .map((s) => {
        const propios = s.puntos.map((p) => aEpoch(p.instante));
        const cercano = indiceMasCercano(propios, objetivo);
        const punto = cercano >= 0 ? s.puntos[cercano] : null;
        const dentroDeRango = punto && Math.abs(propios[cercano] - objetivo) <= tolerancia;
        return {
          etiqueta: s.etiqueta,
          valor: dentroDeRango && punto.valor !== null ? this.formatear(punto.valor) : '—',
        };
      });
  });

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const transmision = Number(params.get('transmision'));
    if (transmision) {
      this.transmisionFiltro.set(transmision);
    }
    this.desdeFiltro.set(params.get('desde') ?? '');
    this.hastaFiltro.set(params.get('hasta') ?? '');
    const agrupacion = params.get('agrupacion');
    if (agrupacion === 'CRUDA' || agrupacion === 'HORA') {
      this.agrupacionFiltro.set(agrupacion);
    }
    this.cargarCatalogo();
  }

  cargarCatalogo(): void {
    this.cargandoCatalogo.set(true);
    this.errorCatalogo.set(null);
    this.analytics.transmisiones().subscribe({
      next: (lista) => {
        this.transmisiones.set(lista);
        this.cargandoCatalogo.set(false);
        if (this.transmisionFiltro() === null) {
          const candidata = lista.find((t) => t.muestras > 0);
          if (candidata) {
            this.transmisionFiltro.set(candidata.id);
          }
        }
        if (this.transmisionFiltro() !== null) {
          this.buscar();
        }
      },
      error: () => {
        this.errorCatalogo.set('No se pudieron cargar las transmisiones.');
        this.cargandoCatalogo.set(false);
      },
    });
  }

  buscar(): void {
    const transmisionId = this.transmisionFiltro();
    if (transmisionId === null) {
      return;
    }
    if (this.rangoInvalido()) {
      this.error.set('La fecha "desde" no puede ser posterior a "hasta".');
      return;
    }

    const desde = this.desdeFiltro() ? `${this.desdeFiltro()}T00:00:00` : null;
    const hasta = this.hastaFiltro() ? `${this.hastaFiltro()}T23:59:59` : null;
    this.sincronizarUrl();

    const mio = ++this.token;
    this.cargando.set(true);
    this.error.set(null);
    this.indiceActivo.set(null);
    this.analytics
      .series({ transmisionId, desde, hasta, agrupacion: this.agrupacionFiltro() })
      .subscribe({
        next: (respuesta) => {
          if (mio !== this.token) {
            return;
          }
          this.datos.set(respuesta);
          this.cargando.set(false);
        },
        error: () => {
          if (mio !== this.token) {
            return;
          }
          this.error.set('No se pudieron cargar las métricas.');
          this.cargando.set(false);
        },
      });
  }

  limpiarFiltros(): void {
    this.desdeFiltro.set('');
    this.hastaFiltro.set('');
    this.agrupacionFiltro.set('HORA');
    this.buscar();
  }

  alCambiarTransmision(valor: string): void {
    this.transmisionFiltro.set(valor ? Number(valor) : null);
  }

  exportarCsv(): void {
    const datos = this.datos();
    if (!datos) {
      return;
    }
    const series = datos.series.filter((s) => s.puntos.length);
    const cabecera = ['instante', ...series.map((s) => s.clave)].join(',');
    const valores = new Map<number, Map<string, number | null>>();
    series.forEach((s) =>
      s.puntos.forEach((p) => {
        const t = aEpoch(p.instante);
        if (!valores.has(t)) {
          valores.set(t, new Map());
        }
        valores.get(t)!.set(s.clave, p.valor);
      })
    );

    const filas = [...valores.entries()]
      .sort((a, b) => a[0] - b[0])
      .map(([t, porClave]) =>
        [
          new Date(t).toISOString(),
          ...series.map((s) => {
            const valor = porClave.get(s.clave);
            return valor === null || valor === undefined ? '' : String(valor);
          }),
        ].join(',')
      );

    const blob = new Blob([[cabecera, ...filas].join('\n')], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `metricas-transmision-${datos.transmisionId}.csv`;
    enlace.click();
    URL.revokeObjectURL(url);
  }

  // --- helpers ---

  private serie(clave: ClaveSerie): SerieMetrica | undefined {
    return this.datos()?.series.find((s) => s.clave === clave);
  }

  private tieneDatos(clave: ClaveSerie): boolean {
    return (this.serie(clave)?.puntos.length ?? 0) > 0;
  }

  private aGrafico(
    claves: ClaveSerie[],
    clases: Partial<Record<ClaveSerie, string>>,
    area = false
  ): SerieGrafico[] {
    return claves
      .map((clave) => this.serie(clave))
      .filter((s): s is SerieMetrica => !!s && s.puntos.length > 0)
      .map((s) => ({
        clave: s.clave,
        etiqueta: s.etiqueta,
        clase: clases[s.clave] ?? 'serie-espectadores',
        area,
        puntos: s.puntos.map((p) => ({ t: aEpoch(p.instante), v: p.valor })),
      }));
  }

  private sincronizarUrl(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      replaceUrl: true,
      queryParams: {
        transmision: this.transmisionFiltro(),
        desde: this.desdeFiltro() || null,
        hasta: this.hastaFiltro() || null,
        agrupacion: this.agrupacionFiltro(),
      },
    });
  }

  private formatear(valor: number): string {
    return Number.isInteger(valor) ? String(valor) : valor.toFixed(2);
  }

  /** Para las tarjetas del resumen: un guion cuando no hay dato, nunca un 0 inventado. */
  cifra(valor: number | null, decimales = 0): string {
    return valor === null || valor === undefined ? '—' : valor.toFixed(decimales);
  }
}
