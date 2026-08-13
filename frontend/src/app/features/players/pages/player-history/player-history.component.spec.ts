import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { PlayerHistoryComponent } from './player-history.component';
import { PlayersService } from '../../services/players.service';

describe('PlayerHistoryComponent', () => {
  let component: PlayerHistoryComponent;
  let fixture: ComponentFixture<PlayerHistoryComponent>;

  beforeEach(async () => {
    // El componente pide historial y personalización en paralelo (forkJoin):
    // si al doble le falta una de las dos, no emite y la vista queda cargando.
    const playersServiceMock = {
      historial: () => of([]),
      personalizacion: () => of(null)
    };

    await TestBed.configureTestingModule({
      imports: [PlayerHistoryComponent],
      providers: [
        { provide: PlayersService, useValue: playersServiceMock },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PlayerHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load an empty history without error', () => {
    expect(component.historial().length).toBe(0);
    expect(component.error()).toBeNull();
  });
});
