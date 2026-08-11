import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TournamentsService } from './tournaments.service';
import { environment } from '../../../../environments/environment';
import { Partida, Torneo } from '../../../models/tournament.model';

/**
 * Igual que TwitchService, este servicio no tiene lógica propia: arma la
 * URL y delega en ApiService. Se prueba el contrato HTTP —método, ruta y
 * cuerpo— con HttpTestingController, sin levantar un servidor real.
 */
describe('TournamentsService', () => {
  let service: TournamentsService;
  let http: HttpTestingController;

  const base = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(TournamentsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Falla el test si quedó alguna petición sin atender: así no se cuela un
    // servicio que pega a una URL de más.
    http.verify();
  });

  it('listar() sin juego pide /tournaments sin filtro', () => {
    const torneos: Torneo[] = [];

    service.listar().subscribe();

    const peticion = http.expectOne(`${base}/tournaments`);
    expect(peticion.request.method).toBe('GET');

    peticion.flush(torneos);
  });

  it('listar() con juegoId agrega el query param', () => {
    service.listar(4).subscribe();

    const peticion = http.expectOne(`${base}/tournaments?juegoId=4`);
    expect(peticion.request.method).toBe('GET');

    peticion.flush([]);
  });

  it('crear() manda los datos del torneo en el cuerpo del POST', () => {
    const request = {
      nombre: 'Copa Demo', juegoId: 4, formato: 'ELIMINACION_DIRECTA',
      tamanoEquipo: 2, maxEquipos: 8, fechaInicio: '2026-09-01T13:00:00', publico: true
    };

    service.crear(request as any).subscribe();

    const peticion = http.expectOne(`${base}/tournaments`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(request);

    peticion.flush({ id: 10 });
  });

  it('reportar() manda el marcador en el cuerpo del POST a la ruta de la partida', () => {
    service.reportar(200, 3, 1).subscribe();

    const peticion = http.expectOne(`${base}/tournaments/partidas/200/reporte`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ marcadorA: 3, marcadorB: 1 });

    peticion.flush({ id: 200 });
  });

  it('bracket() devuelve la lista de partidas tal como la manda el backend', () => {
    const partidas: Partida[] = [{
      id: 200, ronda: 1, orden: 0, fase: null, grupo: null,
      equipoAId: 10, equipoANombre: 'Azules', equipoALogo: null,
      equipoBId: 20, equipoBNombre: 'Rojos', equipoBLogo: null,
      marcadorA: 3, marcadorB: 1, ganadorEquipoId: 10, reportadoPorEquipoId: null,
      estado: 'FINALIZADA', bye: false, lobbyNombre: null, lobbyClave: null, siguientePartidaId: null
    }];

    let recibido: Partida[] | undefined;
    service.bracket(5).subscribe((datos) => (recibido = datos));

    http.expectOne(`${base}/tournaments/5/bracket`).flush(partidas);

    expect(recibido?.length).toBe(1);
    expect(recibido?.[0].ganadorEquipoId).toBe(10);
  });

  it('propaga el error del backend al que llama', () => {
    let estado: number | undefined;
    service.obtener(999).subscribe({ error: (err) => (estado = err.status) });

    http.expectOne(`${base}/tournaments/999`)
      .flush({ message: 'El torneo no existe' }, { status: 404, statusText: 'Not Found' });

    expect(estado).toBe(404);
  });
});
