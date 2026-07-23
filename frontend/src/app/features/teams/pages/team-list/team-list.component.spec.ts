import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
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
    expect(texto).toContain('Ningún equipo coincide con los criterios indicados');
  });

  it('envía los filtros seleccionados como query params tras el debounce', fakeAsync(() => {
    responderBusquedaInicial();

    component.filtros.patchValue({ q: 'invencibles', estado: 'ACTIVO' });
    tick(300);

    const req = httpMock.expectOne(
      `${environment.apiUrl}/teams/search?q=invencibles&estado=ACTIVO&page=0&size=12`
    );
    expect(req.request.method).toBe('GET');
    req.flush(paginaVacia);
  }));

  it('no dispara la búsqueda antes de cumplirse el debounce', fakeAsync(() => {
    responderBusquedaInicial();

    component.filtros.patchValue({ q: 'inven' });
    tick(150);
    httpMock.expectNone(`${environment.apiUrl}/teams/search?q=inven&page=0&size=12`);

    tick(150);
    httpMock.expectOne(`${environment.apiUrl}/teams/search?q=inven&page=0&size=12`).flush(paginaVacia);
  }));

  it('cancela el request en vuelo cuando llega una búsqueda más reciente', fakeAsync(() => {
    responderBusquedaInicial();

    component.buscar(0);
    const primera = httpMock.expectOne(`${environment.apiUrl}/teams/search?page=0&size=12`);

    component.buscar(1);
    expect(primera.cancelled).toBeTrue();

    httpMock
      .expectOne(`${environment.apiUrl}/teams/search?page=1&size=12`)
      .flush({ ...paginaVacia, page: 1 });
    expect(component.pagina()?.page).toBe(1);
  }));

  it('muestra un error recuperable cuando la búsqueda falla', () => {
    responderBusquedaInicial();

    component.buscar(0);
    httpMock
      .expectOne(`${environment.apiUrl}/teams/search?page=0&size=12`)
      .flush({ message: 'error' }, { status: 500, statusText: 'Server Error' });

    expect(component.error()).toContain('No se pudo realizar la búsqueda');
  });
});
