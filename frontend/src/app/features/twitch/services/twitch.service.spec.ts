import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TwitchService } from './twitch.service';
import { environment } from '../../../../environments/environment';
import { MetricasTransmision, TransmisionTwitch } from '../../../models/twitch.model';

/**
 * Estos servicios no tienen lógica propia: arman una URL y delegan en
 * ApiService. Por eso lo que se prueba es el contrato HTTP —método, ruta y
 * cuerpo— sin levantar un servidor: HttpTestingController intercepta la
 * petición, se le entrega una respuesta falsa y se verifica que el servicio
 * la propague tal cual.
 */
describe('TwitchService', () => {
  let service: TwitchService;
  let http: HttpTestingController;

  const base = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      // ApiService usa HttpClient por debajo: sin estos dos providers el
      // TestBed falla con un error de inyección poco descriptivo.
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(TwitchService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Falla el test si quedó alguna petición sin atender: así no se cuela un
    // servicio que pega a una URL de más.
    http.verify();
  });

  it('metricas() consulta la transmisión indicada y devuelve los indicadores', () => {
    const respuesta: MetricasTransmision = {
      transmisionId: 7,
      estado: 'FINALIZADA',
      muestras: 5,
      pico: 1820,
      promedio: 934.5,
      duracionMinutos: 240,
      iniciadaEn: '2026-07-24T18:00:00',
      finalizadaEn: '2026-07-24T22:00:00',
      ultimaMuestra: '2026-07-24T21:59:00'
    };

    let recibido: MetricasTransmision | undefined;
    service.metricas(7).subscribe((datos) => (recibido = datos));

    const peticion = http.expectOne(`${base}/twitch/transmisiones/7/metricas`);
    expect(peticion.request.method).toBe('GET');

    peticion.flush(respuesta);
    expect(recibido).toEqual(respuesta);
  });

  it('abiertas() pide la lista de transmisiones con período abierto', () => {
    const lista: TransmisionTwitch[] = [
      { id: 7, twitchStreamId: '317466024803', torneoId: 27, partidaId: null, estado: 'EN_VIVO', iniciadaEn: '2026-07-24T18:00:00' }
    ];

    let recibido: TransmisionTwitch[] | undefined;
    service.abiertas().subscribe((datos) => (recibido = datos));

    const peticion = http.expectOne(`${base}/twitch/transmisiones`);
    expect(peticion.request.method).toBe('GET');

    peticion.flush(lista);
    expect(recibido?.length).toBe(1);
    expect(recibido?.[0].estado).toBe('EN_VIVO');
  });

  it('asociar() manda torneo y partida en el cuerpo del POST', () => {
    service.asociar(27, null).subscribe();

    const peticion = http.expectOne(`${base}/twitch/transmisiones`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ torneoId: 27, partidaId: null });

    peticion.flush({ id: 8, twitchStreamId: null, torneoId: 27, partidaId: null, estado: 'SIN_DATOS_EN_VIVO', iniciadaEn: null });
  });

  it('propaga el error del backend al que llama', () => {
    let estado: number | undefined;
    service.metricas(99).subscribe({ error: (err) => (estado = err.status) });

    http.expectOne(`${base}/twitch/transmisiones/99/metricas`)
      .flush({ message: 'La transmisión no existe.' }, { status: 404, statusText: 'Not Found' });

    expect(estado).toBe(404);
  });
});
