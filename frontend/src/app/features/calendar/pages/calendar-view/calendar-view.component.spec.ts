import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { CalendarViewComponent } from './calendar-view.component';
import { CalendarService } from '../../services/calendar.service';
import { GamesService } from '../../../games/services/games.service';
import { LeaguesService } from '../../../leagues/services/leagues.service';

describe('CalendarViewComponent', () => {
  let component: CalendarViewComponent;
  let fixture: ComponentFixture<CalendarViewComponent>;

  beforeEach(async () => {
    const calendarServiceMock = { consultar: () => of([]) };
    const gamesServiceMock = { listActivos: () => of([]) };
    const leaguesServiceMock = { list: () => of([]) };

    await TestBed.configureTestingModule({
      imports: [CalendarViewComponent],
      providers: [
        { provide: CalendarService, useValue: calendarServiceMock },
        { provide: GamesService, useValue: gamesServiceMock },
        { provide: LeaguesService, useValue: leaguesServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CalendarViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load an empty calendar without error', () => {
    expect(component.eventos().length).toBe(0);
    expect(component.error()).toBeNull();
  });
});
