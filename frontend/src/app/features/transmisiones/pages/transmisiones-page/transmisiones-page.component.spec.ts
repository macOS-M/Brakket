import { ComponentFixture, TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TransmisionesPageComponent } from './transmisiones-page.component';
import { TransmisionesRespuesta } from '../../../../models/transmision.model';
import { environment } from '../../../../../environments/environment';

const URL = `${environment.apiUrl}/transmisiones`;

const RESPUESTA_EN_VIVO: TransmisionesRespuesta = {
  transmisiones: [
    {
      plataforma: 'TWITCH',
      loginCanal: 'brakketcenfotec',
      nombreCanal: 'BrakketCenfotec',
      avatarUrl: 'https://cdn/avatar.png',
      urlCanal: 'https://www.twitch.tv/brakketcenfotec',
      estado: 'EN_VIVO',
      titulo: 'Gran final del torneo',
      espectadores: 42,
      thumbnailUrl: 'https://cdn/thumb.jpg',
      categoria: 'League of Legends',
      idioma: 'es',
      iniciadaEn: '2026-07-23T18:00:00',
      destacada: true,
      torneoId: null,
      nombreTorneo: null,
      vod: null
    }
  ],
  actualizadoEn: '2026-07-23T18:30:00',
  degradado: false
};

const RESPUESTA_VACIA: TransmisionesRespuesta = {
  transmisiones: [],
  actualizadoEn: '2026-07-23T18:30:00',
  degradado: false
};

describe('TransmisionesPageComponent', () => {
  let fixture: ComponentFixture<TransmisionesPageComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransmisionesPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(TransmisionesPageComponent);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // El poller periódico puede dejar UNA request en vuelo al cortar el test
    // (artefacto de timer() bajo fakeAsync): se descarta y se verifica que no
    // quede ninguna otra request inesperada.
    http.match(URL);
    http.verify();
  });

  it('muestra skeletons mientras carga', fakeAsync(() => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.skeleton').length).toBeGreaterThan(0);

    tick();
    http.expectOne(URL).flush(RESPUESTA_VACIA);
    discardPeriodicTasks();
  }));

  it('pinta el hero, la tarjeta real y el relleno "próximamente" con datos', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    http.expectOne(URL).flush(RESPUESTA_EN_VIVO);
    fixture.detectChanges();

    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Gran final del torneo');
    expect(texto).toContain('EN VIVO');
    expect(texto).toContain('42');
    // Solo la transmisión real: la grilla ya no se rellena con tarjetas
    // "Próximamente", que prometían directos de la comunidad inexistentes.
    expect(fixture.nativeElement.querySelectorAll('app-stream-card').length).toBe(1 + 1); // +1 en "Por juego"
    expect(fixture.nativeElement.querySelectorAll('.tarjeta-proximamente').length).toBe(0);
    // La sección por juego se agrupa desde la misma estructura de datos.
    expect(texto).toContain('League of Legends');
    discardPeriodicTasks();
  }));

  it('muestra el estado vacío sin transmisiones', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    http.expectOne(URL).flush(RESPUESTA_VACIA);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Sin transmisiones por ahora');
    discardPeriodicTasks();
  }));

  it('muestra el estado de error y permite reintentar', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    http.expectOne(URL).flush('caído', { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No pudimos cargar las transmisiones');

    fixture.nativeElement.querySelector('.boton').click();
    http.expectOne(URL).flush(RESPUESTA_EN_VIVO);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Gran final del torneo');
    discardPeriodicTasks();
  }));

  it('avisa cuando la respuesta llega degradada', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    http.expectOne(URL).flush({
      ...RESPUESTA_EN_VIVO,
      degradado: true,
      transmisiones: [{ ...RESPUESTA_EN_VIVO.transmisiones[0], estado: 'DESCONOCIDO', espectadores: null }]
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.aviso')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Twitch no está respondiendo');
    discardPeriodicTasks();
  }));
});
