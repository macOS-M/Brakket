import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { TransferFormComponent } from './transfer-form.component';

describe('TransferFormComponent', () => {
  let component: TransferFormComponent;
  let fixture: ComponentFixture<TransferFormComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransferFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(TransferFormComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  function responderEquipos(): void {
    httpMock.expectOne(`${environment.apiUrl}/public/teams?criterio=`).flush([
      { id: 10, nombre: 'Origen FC', juegoNombre: null, integrantesActivos: 3 },
      { id: 20, nombre: 'Destino FC', juegoNombre: null, integrantesActivos: 2 }
    ]);
  }

  it('should create', () => {
    responderEquipos();
    expect(component).toBeTruthy();
  });

  it('el formulario es inválido sin jugador, origen y destino', () => {
    responderEquipos();
    expect(component.form.invalid).toBeTrue();
  });

  it('al elegir equipo de origen carga sus integrantes y excluye al capitán', () => {
    responderEquipos();

    component.form.controls.equipoOrigenId.setValue('10');
    httpMock.expectOne(`${environment.apiUrl}/teams/10/miembros`).flush([
      { id: 1, equipoId: 10, usuarioId: 2, nombreUsuario: 'Capi', rol: 'CAPITAN', estado: 'ACTIVO' },
      { id: 2, equipoId: 10, usuarioId: 3, nombreUsuario: 'Jugador', rol: 'TITULAR', estado: 'ACTIVO' },
      { id: 3, equipoId: 10, usuarioId: 4, nombreUsuario: 'Inactivo', rol: 'TITULAR', estado: 'INACTIVO' }
    ]);

    expect(component.miembrosOrigen()).toHaveSize(1);
    expect(component.miembrosOrigen()[0].nombreUsuario).toBe('Jugador');
  });

  it('envía la solicitud con los datos del formulario', () => {
    responderEquipos();

    component.form.controls.equipoOrigenId.setValue('10');
    httpMock.expectOne(`${environment.apiUrl}/teams/10/miembros`).flush([
      { id: 2, equipoId: 10, usuarioId: 3, nombreUsuario: 'Jugador', rol: 'TITULAR', estado: 'ACTIVO' }
    ]);
    component.form.patchValue({ jugadorId: '3', equipoDestinoId: '20' });

    component.guardar();

    const req = httpMock.expectOne(`${environment.apiUrl}/transfers`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      jugadorId: 3,
      equipoOrigenId: 10,
      equipoDestinoId: 20,
      rolPropuesto: 'TITULAR',
      justificacion: null
    });
    req.flush({});
  });
});
