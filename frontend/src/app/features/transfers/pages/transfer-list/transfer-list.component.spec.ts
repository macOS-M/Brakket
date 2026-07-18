import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { Transferencia } from '../../../../models/transferencia.model';
import { TransferListComponent } from './transfer-list.component';

describe('TransferListComponent', () => {
  let component: TransferListComponent;
  let fixture: ComponentFixture<TransferListComponent>;
  let httpMock: HttpTestingController;

  const transferencia: Transferencia = {
    id: 1,
    jugadorId: 3,
    jugadorNombre: 'Jugador Estrella',
    equipoOrigenId: 10,
    equipoOrigenNombre: 'Origen FC',
    equipoDestinoId: 20,
    equipoDestinoNombre: 'Destino FC',
    solicitanteId: 1,
    solicitanteNombre: 'Capi Destino',
    rolPropuesto: 'TITULAR',
    justificacion: null,
    estado: 'PENDIENTE',
    aprobacionJugador: 'PENDIENTE',
    aprobacionCapitanOrigen: 'PENDIENTE',
    fechaSolicitud: '2026-07-13T10:00:00',
    fechaResolucion: null
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransferListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(TransferListComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  function responderCarga(
    pendientes: Transferencia[] = [],
    enviadas: Transferencia[] = []
  ): void {
    httpMock.expectOne(`${environment.apiUrl}/transfers/pendientes`).flush(pendientes);
    httpMock.expectOne(`${environment.apiUrl}/transfers/enviadas`).flush(enviadas);
  }

  it('should create', () => {
    responderCarga();
    expect(component).toBeTruthy();
  });

  it('muestra pendientes por responder y enviadas por separado', () => {
    responderCarga([transferencia], [transferencia]);
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Pendientes de mi respuesta');
    expect(texto).toContain('Solicitudes enviadas');
    expect(texto).toContain('Origen FC → Destino FC');
    expect(component.pendientes()).toHaveSize(1);
    expect(component.enviadas()).toHaveSize(1);
  });

  it('responder envía la decisión y recarga las listas', () => {
    responderCarga([transferencia]);

    component.responder(transferencia, 'ACEPTAR');

    const req = httpMock.expectOne(`${environment.apiUrl}/transfers/1/responder`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ decision: 'ACEPTAR' });
    req.flush({ ...transferencia, aprobacionJugador: 'ACEPTADA' });

    // Tras responder se recargan ambas listas.
    responderCarga();
    expect(component.mensaje()).toContain('falta la aprobación');
  });

  it('muestra el error del backend cuando la respuesta falla', () => {
    responderCarga([transferencia]);

    component.responder(transferencia, 'RECHAZAR');
    httpMock
      .expectOne(`${environment.apiUrl}/transfers/1/responder`)
      .flush(
        { success: false, message: 'La solicitud ya fue resuelta' },
        { status: 409, statusText: 'Conflict' }
      );

    expect(component.error()).toContain('ya fue resuelta');
  });

  it('muestra un error recuperable cuando la carga falla', () => {
    httpMock
      .expectOne(`${environment.apiUrl}/transfers/pendientes`)
      .flush({ message: 'error' }, { status: 500, statusText: 'Server Error' });

    expect(component.error()).toContain('No se pudieron cargar');
  });
});
