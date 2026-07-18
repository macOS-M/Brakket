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

  it('should create', () => {
    httpMock.expectOne(`${environment.apiUrl}/transfers/enviadas`).flush([]);
    expect(component).toBeTruthy();
  });

  it('muestra las solicitudes enviadas con su estado y aprobaciones', () => {
    httpMock.expectOne(`${environment.apiUrl}/transfers/enviadas`).flush([transferencia]);
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Jugador Estrella');
    expect(texto).toContain('Origen FC → Destino FC');
    expect(texto).toContain('Pendiente');
  });

  it('muestra un error recuperable cuando la carga falla', () => {
    httpMock
      .expectOne(`${environment.apiUrl}/transfers/enviadas`)
      .flush({ message: 'error' }, { status: 500, statusText: 'Server Error' });

    expect(component.error()).toContain('No se pudieron cargar');
  });
});
