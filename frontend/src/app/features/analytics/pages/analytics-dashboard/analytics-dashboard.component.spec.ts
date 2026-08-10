import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AnalyticsDashboardComponent } from './analytics-dashboard.component';
import { AnalyticsService, FiltrosSeries } from '../../services/analytics.service';
import {
  SerieMetrica,
  SeriesTransmision,
  TransmisionAnalizable,
} from '../../../../models/analitica.model';

describe('AnalyticsDashboardComponent', () => {
  let component: AnalyticsDashboardComponent;
  let fixture: ComponentFixture<AnalyticsDashboardComponent>;

  const TRANSMISION: TransmisionAnalizable = {
    id: 7,
    etiqueta: 'Copa Brakket — 24/07 18:00',
    torneoId: 3,
    nombreTorneo: 'Copa Brakket',
    estado: 'EN_VIVO',
    iniciadaEn: '2026-07-24T18:00:00',
    finalizadaEn: null,
    muestras: 120
  };

  function serie(clave: SerieMetrica['clave'], valores: number[]): SerieMetrica {
    return {
      clave,
      etiqueta: clave,
      unidad: null,
      muestras: valores.length,
      promedio: valores.length ? valores.reduce((a, b) => a + b, 0) / valores.length : null,
      pico: valores.length ? Math.max(...valores) : null,
      minimo: valores.length ? Math.min(...valores) : null,
      puntos: valores.map((v, i) => ({
        instante: `2026-07-24T18:0${i}:00`,
        valor: v
      }))
    };
  }

  function respuesta(): SeriesTransmision {
    return {
      transmisionId: 7,
      etiquetaTransmision: 'Copa Brakket',
      estado: 'EN_VIVO',
      agrupacion: 'HORA',
      desde: null,
      hasta: null,
      duracionMinutos: 120,
      intervaloSegundos: 60,
      origen: 'REAL',
      resumen: {
        muestrasAudiencia: 3,
        picoEspectadores: 300,
        promedioEspectadores: 200,
        muestrasChat: 0,
        promedioMensajesPorMinuto: null,
        picoUsuariosActivos: null,
        muestrasSentimiento: 0,
        promedioPuntaje: null,
        clasificacionPredominante: null
      },
      // El sentimiento viaja presente pero vacío: es el estado real hasta RF-39.
      series: [
        serie('ESPECTADORES', [100, 200, 300]),
        serie('MENSAJES_POR_MINUTO', []),
        serie('USUARIOS_ACTIVOS', []),
        serie('SENTIMIENTO', [])
      ]
    };
  }

  let transmisionesMock: () => Observable<TransmisionAnalizable[]>;
  let seriesMock: (filtros: FiltrosSeries) => Observable<SeriesTransmision>;
  let ultimosFiltros: FiltrosSeries | null;
  let llamadasASeries: number;

  async function montar(queryParams: Record<string, string> = {}): Promise<void> {
    const analyticsServiceMock = {
      transmisiones: () => transmisionesMock(),
      series: (filtros: FiltrosSeries) => {
        llamadasASeries++;
        ultimosFiltros = filtros;
        return seriesMock(filtros);
      }
    };

    await TestBed.configureTestingModule({
      imports: [AnalyticsDashboardComponent],
      providers: [
        { provide: AnalyticsService, useValue: analyticsServiceMock },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    ultimosFiltros = null;
    llamadasASeries = 0;
    transmisionesMock = () => of([TRANSMISION]);
    seriesMock = () => of(respuesta());
  });

  it('autoselecciona la primera transmision con datos y consulta sus metricas', async () => {
    await montar();

    expect(component.transmisionFiltro()).toBe(7);
    expect(llamadasASeries).toBe(1);
    expect(component.datos()).not.toBeNull();
  });

  it('no consulta metricas si ninguna transmision tiene muestras', async () => {
    transmisionesMock = () => of([{ ...TRANSMISION, muestras: 0 }]);

    await montar();

    expect(component.transmisionFiltro()).toBeNull();
    expect(llamadasASeries).toBe(0);
  });

  it('manda el rango con los limites del dia y la agrupacion elegida', async () => {
    await montar();
    component.desdeFiltro.set('2026-07-24');
    component.hastaFiltro.set('2026-07-25');
    component.agrupacionFiltro.set('CRUDA');

    component.buscar();

    expect(ultimosFiltros?.desde).toBe('2026-07-24T00:00:00');
    expect(ultimosFiltros?.hasta).toBe('2026-07-25T23:59:59');
    expect(ultimosFiltros?.agrupacion).toBe('CRUDA');
  });

  it('no consulta el backend con un rango invertido', async () => {
    await montar();
    const previas = llamadasASeries;
    component.desdeFiltro.set('2026-07-25');
    component.hastaFiltro.set('2026-07-24');

    component.buscar();

    expect(component.rangoInvalido()).toBeTrue();
    expect(llamadasASeries).toBe(previas);
    expect(component.error()).not.toBeNull();
  });

  it('hidrata los filtros desde la URL', async () => {
    await montar({
      transmision: '7',
      desde: '2026-07-24',
      hasta: '2026-07-24',
      agrupacion: 'CRUDA'
    });

    expect(component.transmisionFiltro()).toBe(7);
    expect(component.desdeFiltro()).toBe('2026-07-24');
    expect(component.agrupacionFiltro()).toBe('CRUDA');
  });

  it('muestra la audiencia aunque el sentimiento venga vacio', async () => {
    await montar();

    expect(component.audienciaDisponible()).toBeTrue();
    expect(component.chatDisponible()).toBeFalse();
    expect(component.sentimientoDisponible()).toBeFalse();
    expect(component.graficoAudiencia().length).toBe(1);
    expect(component.graficoSentimiento().length).toBe(0);
  });

  it('tolera que el backend omita una clave de serie', async () => {
    seriesMock = () =>
      of({ ...respuesta(), series: [serie('ESPECTADORES', [10, 20])] });

    await montar();

    expect(component.sentimientoDisponible()).toBeFalse();
    expect(component.audienciaDisponible()).toBeTrue();
  });

  it('comparte el eje X entre las series', async () => {
    await montar();

    expect(component.instantes().length).toBe(3);
  });

  it('reporta el error del backend sin quedarse cargando', async () => {
    seriesMock = () => throwError(() => new Error('500'));

    await montar();

    expect(component.error()).not.toBeNull();
    expect(component.cargando()).toBeFalse();
  });

  it('limpiarFiltros vuelve a agrupacion por hora y reconsulta', async () => {
    await montar();
    component.desdeFiltro.set('2026-07-24');
    component.agrupacionFiltro.set('CRUDA');
    const previas = llamadasASeries;

    component.limpiarFiltros();

    expect(component.desdeFiltro()).toBe('');
    expect(component.agrupacionFiltro()).toBe('HORA');
    expect(llamadasASeries).toBe(previas + 1);
  });
});
