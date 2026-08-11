import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TermometroPageComponent } from './termometro-page.component';
import { Termometro, TransmisionAnalizada } from '../../../../models/sentiment.model';

describe('TermometroPageComponent', () => {
  let component: TermometroPageComponent;
  let fixture: ComponentFixture<TermometroPageComponent>;
  let http: HttpTestingController;

  const transmision = (id: number, muestras = 12): TransmisionAnalizada => ({
    id,
    estado: 'EN_VIVO',
    iniciadaEn: '2026-08-10T18:00:00',
    torneoId: 3,
    totalMuestras: muestras
  });

  const termometro = (): Termometro => ({
    transmisionId: 4,
    estado: 'DISPONIBLE',
    resumen: 'El ambiente del chat es mayormente positivo.',
    desde: null,
    hasta: null,
    intervaloMinutos: 15,
    puntajeGeneral: 42,
    clasificacion: 'POSITIVO',
    totalMuestras: 12,
    minimoMuestras: 3,
    distribucion: {
      positivo: 8, neutro: 2, negativo: 2,
      porcentajePositivo: 66.67, porcentajeNeutro: 16.67, porcentajeNegativo: 16.67
    },
    intervalos: []
  });

  /** Responde el listado que dispara ngOnInit. */
  const responderListado = (ts: TransmisionAnalizada[]) => {
    http.expectOne((r) => r.url.endsWith('/analytics/transmisiones')).flush(ts);
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TermometroPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(TermometroPageComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('should create', () => {
    responderListado([]);
    expect(component).toBeTruthy();
  });

  it('carga las transmisiones analizadas al iniciar', () => {
    responderListado([transmision(4), transmision(9)]);

    expect(component.transmisiones().length).toBe(2);
    // Con varias no se elige ninguna: la decisión es de quien consulta.
    expect(component.transmisionId).toBeNull();
  });

  it('preselecciona y consulta cuando hay una sola transmisión', () => {
    responderListado([transmision(4)]);

    expect(component.transmisionId).toBe(4);
    const peticion = http.expectOne((r) => r.url.includes('/sentimiento/termometro'));
    peticion.flush(termometro());

    expect(component.termometro()?.puntajeGeneral).toBe(42);
  });

  it('exige elegir una transmisión antes de consultar', () => {
    responderListado([transmision(4), transmision(9)]);
    component.transmisionId = null;

    component.consultar();

    expect(component.error()).toContain('Elegí la transmisión');
  });

  it('manda el intervalo del período elegido', () => {
    responderListado([transmision(4), transmision(9)]);
    component.transmisionId = 4;
    component.periodo = component.periodos[1]; // Últimas 6 horas → 15 min

    component.consultar();

    const peticion = http.expectOne((r) => r.url.includes('/sentimiento/termometro'));
    expect(peticion.request.urlWithParams).toContain('intervaloMinutos=15');
    peticion.flush(termometro());
  });

  it('manda el inicio del período sin zona horaria', () => {
    responderListado([transmision(4), transmision(9)]);
    component.transmisionId = 4;
    component.periodo = component.periodos[0]; // Última hora

    component.consultar();

    const peticion = http.expectOne((r) => r.url.includes('/sentimiento/termometro'));
    const desde = new URL('http://x' + peticion.request.urlWithParams).searchParams.get('desde');
    // El backend recibe LocalDateTime: un ISO con Z lo correría por el desfase.
    expect(desde).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/);
    peticion.flush(termometro());
  });

  it('el período completo no manda desde', () => {
    responderListado([transmision(4), transmision(9)]);
    component.transmisionId = 4;
    component.periodo = component.periodos[3]; // Toda la transmisión

    component.consultar();

    const peticion = http.expectOne((r) => r.url.includes('/sentimiento/termometro'));
    expect(peticion.request.urlWithParams).not.toContain('desde=');
    peticion.flush(termometro());
  });

  it('traduce el 403 a un mensaje de permisos', () => {
    responderListado([transmision(4), transmision(9)]);
    component.transmisionId = 4;

    component.consultar();

    http.expectOne((r) => r.url.includes('/sentimiento/termometro'))
      .flush({}, { status: 403, statusText: 'Forbidden' });

    expect(component.error()).toContain('permiso');
    expect(component.termometro()).toBeNull();
  });

  it('arma una etiqueta legible para el desplegable', () => {
    responderListado([]);
    expect(component.etiqueta(transmision(7, 20))).toBe('#7 · EN_VIVO · torneo 3 · 20 muestras');
  });
});
