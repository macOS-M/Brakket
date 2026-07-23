import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { TeamFormComponent } from './team-form.component';
import { TeamsService } from '../../services/teams.service';
import { GamesService } from '../../../games/services/games.service';

describe('TeamFormComponent', () => {
  let component: TeamFormComponent;
  let fixture: ComponentFixture<TeamFormComponent>;

  beforeEach(async () => {
    const gamesServiceMock = { listActivos: () => of([]) };
    const teamsServiceMock = { crear: () => of({ id: 1 }) };
    const routerMock = { navigate: () => Promise.resolve(true) };
    // Sin equipoId en la ruta: el form arranca en modo "crear".
    const routeMock = { snapshot: { paramMap: { get: () => null } } };

    await TestBed.configureTestingModule({
      imports: [TeamFormComponent],
      providers: [
        // El foto-input del formulario inyecta UploadsService -> HttpClient.
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: GamesService, useValue: gamesServiceMock },
        { provide: TeamsService, useValue: teamsServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: routeMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TeamFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should be invalid when nombre and juegoId are empty', () => {
    expect(component.form.invalid).toBeTrue();
  });

  it('should add and remove a red social control', () => {
    component.agregarRedSocial();
    expect(component.redesSociales.length).toBe(1);
    component.quitarRedSocial(0);
    expect(component.redesSociales.length).toBe(0);
  });
});
