import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
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

    await TestBed.configureTestingModule({
      imports: [TeamFormComponent],
      providers: [
        { provide: GamesService, useValue: gamesServiceMock },
        { provide: TeamsService, useValue: teamsServiceMock },
        { provide: Router, useValue: routerMock }
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
