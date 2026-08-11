import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AnalyticsDashboardComponent } from './analytics-dashboard.component';
import { TransmisionTwitch } from '../../../../models/twitch.model';

describe('AnalyticsDashboardComponent', () => {
  let component: AnalyticsDashboardComponent;
  let fixture: ComponentFixture<AnalyticsDashboardComponent>;
  let http: HttpTestingController;

  const transmision = (id: number): TransmisionTwitch => ({
    id,
    twitchStreamId: `stream-${id}`,
    torneoId: 3,
    partidaId: null,
    estado: 'EN_VIVO',
    iniciadaEn: '2026-08-07T18:00:00'
  });

  /** Responde la carga de transmisiones que dispara ngOnInit. */
  const responderTransmisiones = (ts: TransmisionTwitch[]) => {
    http.expectOne((r) => r.url.endsWith('/twitch/transmisiones')).flush(ts);
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalyticsDashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('should create', () => {
    responderTransmisiones([]);
    expect(component).toBeTruthy();
  });

  it('carga las transmisiones abiertas al iniciar', () => {
    responderTransmisiones([transmision(4), transmision(9)]);
    expect(component.transmisiones().length).toBe(2);
    // Con varias abiertas no se elige ninguna: la decisión es del admin.
    expect(component.transmisionId).toBeNull();
  });

  it('preselecciona la transmisión cuando hay una sola', () => {
    responderTransmisiones([transmision(12)]);
    expect(component.transmisionId).toBe(12);
  });

  it('exige elegir la transmisión antes de analizar', () => {
    responderTransmisiones([]);
    component.transmisionId = null;
    component.mensajesTexto = 'gg';
    component.analizar();
    expect(component.error()).toContain('transmisión');
  });

  it('exige al menos un mensaje de chat', () => {
    responderTransmisiones([]);
    component.transmisionId = 1;
    component.mensajesTexto = '   ';
    component.analizar();
    expect(component.error()).toContain('mensaje');
  });

  it('rechaza en el cliente un lote por encima del tope del backend', () => {
    responderTransmisiones([]);
    component.transmisionId = 1;
    component.mensajesTexto = Array(component.maxMensajes + 1).fill('gg').join('\n');
    component.analizar();
    expect(component.error()).toContain(`${component.maxMensajes}`);
  });

  it('arma una etiqueta legible para el desplegable', () => {
    responderTransmisiones([]);
    expect(component.etiqueta(transmision(7))).toBe('#7 · EN_VIVO · torneo 3');
  });
});
