import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { TeamRosterComponent } from './team-roster.component';
import { TeamsService } from '../../services/teams.service';

describe('TeamRosterComponent', () => {
  let component: TeamRosterComponent;
  let fixture: ComponentFixture<TeamRosterComponent>;
  let teamsService: TeamsService;

  const equipoDisuelto = {
    id: 1,
    nombre: 'Coffee&Commits',
    logo: null,
    descripcion: null,
    estado: 'DISUELTO',
    fechaDisolucion: '2026-07-10T12:00:00',
    motivoDisolucion: 'Fin de temporada'
  };

  beforeEach(async () => {
    const teamsServiceMock = {
      listMiembros: () => of([]),
      cambiarRol: () => of({}),
      disolver: () => of(equipoDisuelto)
    };

    await TestBed.configureTestingModule({
      imports: [TeamRosterComponent],
      providers: [
        { provide: TeamsService, useValue: teamsServiceMock },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TeamRosterComponent);
    component = fixture.componentInstance;
    teamsService = TestBed.inject(TeamsService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load an empty roster without error', () => {
    expect(component.miembros().length).toBe(0);
    expect(component.error()).toBeNull();
  });

  it('should not dissolve the team without explicit confirmation', () => {
    const spy = spyOn(teamsService, 'disolver').and.callThrough();

    component.disolverEquipo();

    expect(spy).not.toHaveBeenCalled();
    expect(component.equipoDisuelto()).toBeNull();
  });

  it('should dissolve the team when confirmed, sending the trimmed optional reason', () => {
    const spy = spyOn(teamsService, 'disolver').and.callThrough();
    component.confirmaDisolucion.set(true);
    component.motivoDisolucion.set('  Fin de temporada  ');

    component.disolverEquipo();

    expect(spy).toHaveBeenCalledWith(1, { confirmacion: true, motivo: 'Fin de temporada' });
    expect(component.equipoDisuelto()?.estado).toBe('DISUELTO');
  });

  it('should send a null reason when the reason is left empty', () => {
    const spy = spyOn(teamsService, 'disolver').and.callThrough();
    component.confirmaDisolucion.set(true);

    component.disolverEquipo();

    expect(spy).toHaveBeenCalledWith(1, { confirmacion: true, motivo: null });
  });
});
