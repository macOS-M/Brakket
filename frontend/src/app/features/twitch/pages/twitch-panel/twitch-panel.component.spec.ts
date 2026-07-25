import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TwitchPanelComponent } from './twitch-panel.component';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../../../environments/environment';

describe('TwitchPanelComponent', () => {
  let component: TwitchPanelComponent;
  let fixture: ComponentFixture<TwitchPanelComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TwitchPanelComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(TwitchPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    http = TestBed.inject(HttpTestingController);
  });

  it('should create', () => {
    http.expectOne(`${environment.apiUrl}/twitch`).flush({
      id: null, twitchUsuarioId: null, loginCanal: null, nombreMostrado: null,
      urlCanal: null, estado: 'SIN_CONFIGURAR', activo: false, ultimoError: null,
      ultimaValidacion: null, credencialesConfiguradas: false
    });
    expect(component).toBeTruthy();
  });

  it('muestra las métricas de audiencia de la transmisión asociada (RF-36)', () => {
    http.expectOne(`${environment.apiUrl}/twitch`).flush({
      id: 1, twitchUsuarioId: '123', loginCanal: 'brakketcenfotec', nombreMostrado: 'BrakketCenfotec',
      urlCanal: 'https://www.twitch.tv/brakketcenfotec', estado: 'ACTIVO', activo: true,
      ultimoError: null, ultimaValidacion: null, credencialesConfiguradas: true
    });
    component.transmision = {
      id: 7, twitchStreamId: '999', torneoId: 1, partidaId: null,
      estado: 'EN_VIVO', iniciadaEn: '2026-07-24T18:00:00'
    };

    component.cargarMetricas();
    http.expectOne(`${environment.apiUrl}/twitch/transmisiones/7/metricas`).flush({
      transmisionId: 7, estado: 'EN_VIVO', muestras: 3, pico: 57, promedio: 44.3,
      duracionMinutos: 12, iniciadaEn: '2026-07-24T18:00:00', finalizadaEn: null,
      ultimaMuestra: '2026-07-24T18:12:00'
    });
    fixture.detectChanges();

    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Métricas de audiencia');
    expect(texto).toContain('57');
    expect(texto).toMatch(/44[.,]3/); // separador decimal según locale del TestBed
    expect(texto).toContain('12 min');
  });
});
