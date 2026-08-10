import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { StatisticsViewComponent } from './statistics-view.component';
import { StatisticsService } from '../../services/statistics.service';
import { GamesService } from '../../../games/services/games.service';

describe('StatisticsViewComponent', () => {
  let component: StatisticsViewComponent;
  let fixture: ComponentFixture<StatisticsViewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatisticsViewComponent],
      providers: [
        { provide: StatisticsService, useValue: { catalogo: () => of({ ligas: [], temporadas: [] }), buscarSujetos: () => of({ items: [], pagina: 0, tamano: 10, total: 0 }), consultar: () => of(null) } },
        { provide: GamesService, useValue: { listActivos: () => of([]) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StatisticsViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
