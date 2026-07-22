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
      { id: 1, nombre: 'Copa 5v5', tamanoEquipo: 5 },
      { id: 2, nombre: 'Duelo 1v1', tamanoEquipo: 1 }
    ]);

    component.filtrar(5);
    expect(component.filtrados().length).toBe(1);
    expect(component.filtrados()[0].nombre).toBe('Copa 5v5');

    // Volver a tocar el mismo filtro lo apaga.
    component.filtrar(5);
    expect(component.filtrados().length).toBe(2);
  });
});
