import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../../../environments/environment';
import { TournamentListComponent } from './tournament-list.component';

describe('TournamentListComponent', () => {
  let component: TournamentListComponent;
  let fixture: ComponentFixture<TournamentListComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TournamentListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(TournamentListComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    httpMock.expectOne(`${environment.apiUrl}/tournaments`).flush([]);
    expect(component).toBeTruthy();
    expect(component.torneos()).toEqual([]);
  });

  it('filtra por jugadores por equipo', () => {
    httpMock.expectOne(`${environment.apiUrl}/tournaments`).flush([
      { id: 1, nombre: 'Copa 5v5', tamanoEquipo: 5, estado: 'EN_CURSO', fechaInicio: '2020-01-01T00:00:00' },
      { id: 2, nombre: 'Duelo 1v1', tamanoEquipo: 1, estado: 'EN_CURSO', fechaInicio: '2020-01-01T00:00:00' }
    ]);

    // Se aísla el filtro de tamaño con estado 'todos'.
    component.filtrarEstado('todos');
    component.filtrar(5);
    expect(component.filtrados().length).toBe(1);
    expect(component.filtrados()[0].nombre).toBe('Copa 5v5');

    // Volver a tocar el mismo filtro lo apaga.
    component.filtrar(5);
    expect(component.filtrados().length).toBe(2);
  });

  it('distingue abierto (fecha futura) de comenzó (fecha pasada) y ordena abiertos arriba', () => {
    httpMock.expectOne(`${environment.apiUrl}/tournaments`).flush([
      { id: 1, nombre: 'Abierto', tamanoEquipo: 1, estado: 'INSCRIPCION_ABIERTA', fechaInicio: '2099-01-01T00:00:00' },
      { id: 2, nombre: 'Comenzo', tamanoEquipo: 1, estado: 'INSCRIPCION_ABIERTA', fechaInicio: '2020-01-01T00:00:00' },
      { id: 3, nombre: 'EnCurso', tamanoEquipo: 1, estado: 'EN_CURSO', fechaInicio: '2020-01-01T00:00:00' },
      { id: 4, nombre: 'Finalizado', tamanoEquipo: 1, estado: 'FINALIZADO', fechaInicio: '2020-01-01T00:00:00' }
    ]);

    // Default 'comenzados': solo el que comenzó (no en curso, ni abierto, ni finalizado).
    expect(component.filtrados().map((t) => t.nombre)).toEqual(['Comenzo']);

    // 'abiertos': solo el de fecha futura (no el que ya comenzó).
    component.filtrarEstado('abiertos');
    expect(component.filtrados().map((t) => t.nombre)).toEqual(['Abierto']);

    // 'finalizados': solo el finalizado.
    component.filtrarEstado('finalizados');
    expect(component.filtrados().map((t) => t.nombre)).toEqual(['Finalizado']);

    // 'todos': los cuatro, con el abierto arriba de todo.
    component.filtrarEstado('todos');
    expect(component.filtrados()[0].nombre).toBe('Abierto');
    expect(component.filtrados().length).toBe(4);
  });
});
