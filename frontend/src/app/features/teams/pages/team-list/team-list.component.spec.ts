import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { Pagina } from '../../../../models/equipo.model';
import { EquipoBusqueda } from '../../../../models/equipo.model';
import { TeamListComponent } from './team-list.component';

describe('TeamListComponent', () => {
  let component: TeamListComponent;
  let fixture: ComponentFixture<TeamListComponent>;
  let httpMock: HttpTestingController;

  const paginaVacia: Pagina<EquipoBusqueda> = {
    items: [],
    page: 0,
    size: 12,
    totalElements: 0,
    totalPages: 0
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TeamListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(TeamListComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  function responderBusquedaInicial(pagina: Pagina<EquipoBusqueda> = paginaVacia): void {
    httpMock
      .expectOne(`${environment.apiUrl}/teams/search?page=0&size=12`)
      .flush(pagina);
    httpMock.expectOne(`${environment.apiUrl}/games`).flush([]);
  }

  it('should create', () => {
    responderBusquedaInicial();
    expect(component).toBeTruthy();
  });

  it('busca sin filtros al iniciar y muestra el estado vacío', () => {
    responderBusquedaInicial();
    fixture.detectChanges();

    expect(component.pagina()?.items).toEqual([]);
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('No se encontraron equipos');
  });

  it('envía los filtros seleccionados como query params', () => {
    responderBusquedaInicial();

    component.filtros.patchValue({ q: 'invencibles', estado: 'ACTIVO' });
    component.buscar(0);

    const req = httpMock.expectOne(
      `${environment.apiUrl}/teams/search?q=invencibles&estado=ACTIVO&page=0&size=12`
    );
    req.flush(paginaVacia);
    expect(req.request.method).toBe('GET');
  });

  it('muestra un error recuperable cuando la búsqueda falla', () => {
    responderBusquedaInicial();

    component.buscar(0);
    httpMock
      .expectOne(`${environment.apiUrl}/teams/search?page=0&size=12`)
      .flush({ message: 'error' }, { status: 500, statusText: 'Server Error' });

    expect(component.error()).toContain('No se pudo realizar la búsqueda');
  });
});
