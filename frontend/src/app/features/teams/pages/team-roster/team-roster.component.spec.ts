import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { TeamRosterComponent } from './team-roster.component';
import { TeamsService } from '../../services/teams.service';

describe('TeamRosterComponent', () => {
  let component: TeamRosterComponent;
  let fixture: ComponentFixture<TeamRosterComponent>;

  beforeEach(async () => {
    const teamsServiceMock = {
      listMiembros: () => of([]),
      cambiarRol: () => of({})
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
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load an empty roster without error', () => {
    expect(component.miembros().length).toBe(0);
    expect(component.error()).toBeNull();
  });
});
