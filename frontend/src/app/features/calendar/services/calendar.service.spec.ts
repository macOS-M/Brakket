import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CalendarService } from './calendar.service';
import { environment } from '../../../../environments/environment';
import { CalendarioEvento } from '../../../models/calendario-evento.model';

/**
 * A diferencia de otros servicios, CalendarService sí tiene lógica propia:
 * arma los query params a mano y omite los que vienen vacíos/undefined. Por
 * eso además del contrato HTTP se prueba esa construcción de la URL.
 */
describe('CalendarService', () => {
  let service: CalendarService;
  let http: HttpTestingController;

  const base = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(CalendarService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Falla el test si quedó alguna petición sin atender: así no se cuela un
    // servicio que pega a una URL de más.
    http.verify();
  });

  it('consultar() sin filtros pide /calendar sin query string', () => {
    const eventos: CalendarioEvento[] = [];

    service.consultar({}).subscribe();

    const peticion = http.expectOne(`${base}/calendar`);
    expect(peticion.request.method).toBe('GET');

    peticion.flush(eventos);
  });

  it('consultar() con rango de fechas arma ?desde=...&hasta=...', () => {
    service.consultar({ desde: '2026-08-01', hasta: '2026-08-31' }).subscribe();

    const peticion = http.expectOne(`${base}/calendar?desde=2026-08-01&hasta=2026-08-31`);
    expect(peticion.request.method).toBe('GET');

    peticion.flush([]);
  });

  it('consultar() convierte juegoId y ligaId (numéricos) a texto en la URL', () => {
    service.consultar({ juegoId: 4, ligaId: 12 }).subscribe();

    const peticion = http.expectOne(`${base}/calendar?juegoId=4&ligaId=12`);
    expect(peticion.request.method).toBe('GET');

    peticion.flush([]);
  });

  it('consultar() omite los filtros que vienen null, sin mandarlos vacíos', () => {
    service.consultar({ desde: null, hasta: null, juegoId: null, ligaId: null, estado: 'EN_CURSO' })
      .subscribe();

    // Solo "estado" debe aparecer; los null no deben ensuciar la URL.
    const peticion = http.expectOne(`${base}/calendar?estado=EN_CURSO`);
    expect(peticion.request.method).toBe('GET');

    peticion.flush([]);
  });

  it('consultar() devuelve la lista de eventos tal como la manda el backend', () => {
    const eventos: CalendarioEvento[] = [
      {
        torneoId: 5, nombre: 'Copa Relámpago', juegoId: 4, juegoNombre: 'Rocket League',
        ligaId: 1, ligaNombre: 'Liga Demo', temporadaId: 1,
        fechaInicio: '2026-08-15T13:00:00', fechaFin: null, estado: 'INSCRIPCION_ABIERTA', publico: true
      }
    ];

    let recibido: CalendarioEvento[] | undefined;
    service.consultar({ estado: 'INSCRIPCION_ABIERTA' }).subscribe((datos) => (recibido = datos));

    http.expectOne(`${base}/calendar?estado=INSCRIPCION_ABIERTA`).flush(eventos);

    expect(recibido?.length).toBe(1);
    expect(recibido?.[0].nombre).toBe('Copa Relámpago');
  });

  it('propaga el error del backend al que llama', () => {
    let estado: number | undefined;
    service.consultar({}).subscribe({ error: (err) => (estado = err.status) });

    http.expectOne(`${base}/calendar`)
      .flush({ message: 'Error interno' }, { status: 500, statusText: 'Internal Server Error' });

    expect(estado).toBe(500);
  });
});
