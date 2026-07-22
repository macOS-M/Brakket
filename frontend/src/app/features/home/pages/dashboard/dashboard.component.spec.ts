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

  /**
   * Crea el componente con o sin sesión. Con token, el AuthService dispara
   * GET /me en su constructor y el panel pide las cuatro fuentes; sin token,
   * el panel solo pide las públicas (ligas y juegos).
   */
  function crear(conSesion: boolean): void {
    if (conSesion) {
      localStorage.setItem('brakket.jwt', 'jwt-de-prueba');
    }
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    if (conSesion) {
      httpMock
        .expectOne(`${environment.apiUrl}/me`)
        .flush({ authenticated: true, id: 1, nombre: 'Ana', roles: ['JUGADOR'] });
    }
  }

  /** El panel consulta cinco fuentes en paralelo al iniciar (con sesión). */
  function responderCargaInicial(): void {
    httpMock.expectOne(`${environment.apiUrl}/invitaciones/pendientes`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/transfers/pendientes`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/leagues`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/games`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/tournaments`).flush([]);
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();
  });

  afterEach(() => {
    localStorage.removeItem('brakket.jwt');
    httpMock.verify();
  });

  it('should create', () => {
    crear(true);
    responderCargaInicial();
    expect(component).toBeTruthy();
  });

  it('deja de cargar cuando responden las cuatro fuentes', () => {
    crear(true);
    responderCargaInicial();
    expect(component.cargando()).toBeFalse();
    expect(component.errorGeneral()).toBeFalse();
  });

  it('sigue mostrando el panel si solo falla una fuente', () => {
    crear(true);
    httpMock
      .expectOne(`${environment.apiUrl}/invitaciones/pendientes`)
      .flush({ message: 'error' }, { status: 500, statusText: 'Server Error' });
    httpMock.expectOne(`${environment.apiUrl}/transfers/pendientes`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/leagues`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/games`).flush([{ id: 1, nombre: 'LoL' }]);
    httpMock.expectOne(`${environment.apiUrl}/tournaments`).flush([]);

    expect(component.errorGeneral()).toBeFalse();
    expect(component.invitaciones()).toEqual([]);
    expect(component.juegos().length).toBe(1);
  });

  it('marca error general solo si fallan todas las fuentes', () => {
    crear(true);
    const fallo = { status: 500, statusText: 'Server Error' };
    httpMock.expectOne(`${environment.apiUrl}/invitaciones/pendientes`).flush(null, fallo);
    httpMock.expectOne(`${environment.apiUrl}/transfers/pendientes`).flush(null, fallo);
    httpMock.expectOne(`${environment.apiUrl}/leagues`).flush(null, fallo);
    httpMock.expectOne(`${environment.apiUrl}/games`).flush(null, fallo);
    httpMock.expectOne(`${environment.apiUrl}/tournaments`).flush(null, fallo);

    expect(component.errorGeneral()).toBeTrue();
  });

  it('sin sesión solo consulta las fuentes públicas', () => {
    crear(false);
    // Sin token no debe pedir invitaciones ni transferencias: devolverían
    // 401 y el interceptor expulsaría al visitante hacia el login.
    httpMock.expectOne(`${environment.apiUrl}/leagues`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/games`).flush([]);
    httpMock.expectOne(`${environment.apiUrl}/tournaments`).flush([]);
    httpMock.expectNone(`${environment.apiUrl}/invitaciones/pendientes`);
    httpMock.expectNone(`${environment.apiUrl}/transfers/pendientes`);

    expect(component.cargando()).toBeFalse();
    expect(component.errorGeneral()).toBeFalse();
  });
});
