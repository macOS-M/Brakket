import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { TeamListComponent } from './team-list.component';

describe('TeamListComponent', () => {
  let component: TeamListComponent;
  let fixture: ComponentFixture<TeamListComponent>;
  let httpMock: HttpTestingController;

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

  it('should create', () => {
    httpMock.expectOne(`${environment.apiUrl}/public/teams?criterio=`).flush([]);
    expect(component).toBeTruthy();
  });

  it('carga el listado inicial sin criterio', () => {
    httpMock.expectOne(`${environment.apiUrl}/public/teams?criterio=`).flush([
      { id: 1, nombre: 'Los Invencibles', logo: null, juegoNombre: 'Valorant', integrantesActivos: 5 }
    ]);

    expect(component.equipos().length).toBe(1);
    expect(component.cargando()).toBeFalse();
  });

  it('busca tras el debounce y cancela el request anterior', fakeAsync(() => {
    httpMock.expectOne(`${environment.apiUrl}/public/teams?criterio=`).flush([]);

    component.buscar('inv');
    tick(150);
    component.buscar('inven');
    tick(300);

    // El criterio intermedio nunca genera request (debounce) y solo queda el último.
    httpMock.expectNone(`${environment.apiUrl}/public/teams?criterio=inv`);
    httpMock.expectOne(`${environment.apiUrl}/public/teams?criterio=inven`).flush([]);
    expect(component.error()).toBeNull();
  }));

  it('limpia el error cuando una búsqueda posterior funciona', fakeAsync(() => {
    httpMock
      .expectOne(`${environment.apiUrl}/public/teams?criterio=`)
      .flush({ message: 'error' }, { status: 500, statusText: 'Server Error' });
    expect(component.error()).toContain('No se pudieron cargar');

    component.buscar('ok');
    tick(300);
    httpMock.expectOne(`${environment.apiUrl}/public/teams?criterio=ok`).flush([]);

    expect(component.error()).toBeNull();
    expect(component.cargando()).toBeFalse();
  }));
});
