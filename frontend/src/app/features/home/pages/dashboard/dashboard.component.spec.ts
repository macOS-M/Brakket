import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let httpMock: HttpTestingController;

  /** El panel consulta cuatro fuentes en paralelo al iniciar. */
  function responderCargaInicial(): void {
    httpMock.expectOne(`${environment.apiUrl}/invitaciones/pendientes`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/transfers/pendientes`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/leagues`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/games`).flush([]);
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    responderCargaInicial();
    expect(component).toBeTruthy();
  });

  it('deja de cargar cuando responden las cuatro fuentes', () => {
    responderCargaInicial();
    expect(component.cargando()).toBeFalse();
    expect(component.errorGeneral()).toBeFalse();
  });

  it('sigue mostrando el panel si solo falla una fuente', () => {
    httpMock
      .expectOne(`${environment.apiUrl}/invitaciones/pendientes`)
      .flush({ message: 'error' }, { status: 500, statusText: 'Server Error' });
    httpMock.expectOne(`${environment.apiUrl}/transfers/pendientes`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/leagues`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/games`).flush([{ id: 1, nombre: 'LoL' }]);

    expect(component.errorGeneral()).toBeFalse();
    expect(component.invitaciones()).toEqual([]);
    expect(component.totalJuegos()).toBe(1);
  });

  it('marca error general solo si fallan todas las fuentes', () => {
    const fallo = { status: 500, statusText: 'Server Error' };
    httpMock.expectOne(`${environment.apiUrl}/invitaciones/pendientes`).flush(null, fallo);
    httpMock.expectOne(`${environment.apiUrl}/transfers/pendientes`).flush(null, fallo);
    httpMock.expectOne(`${environment.apiUrl}/leagues`).flush(null, fallo);
    httpMock.expectOne(`${environment.apiUrl}/games`).flush(null, fallo);

    expect(component.errorGeneral()).toBeTrue();
  });
});
