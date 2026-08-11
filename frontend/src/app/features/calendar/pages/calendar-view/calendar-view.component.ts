import { Component, OnInit, inject, signal } from '@angular/core';

import { CalendarioEvento } from '../../../../models/calendario-evento.model';
import { Juego } from '../../../../models/juego.model';
import { League } from '../../../../models/league.model';
import { CalendarService } from '../../services/calendar.service';
import { GamesService } from '../../../games/services/games.service';
import { LeaguesService } from '../../../leagues/services/leagues.service';
import { AdSlotComponent } from '../../../../shared/components/ad-slot/ad-slot.component';

@Component({
  selector: 'app-calendar-view',
  standalone: true,
  imports: [AdSlotComponent],
  templateUrl: './calendar-view.component.html',
  styleUrl: './calendar-view.component.scss'
})
export class CalendarViewComponent implements OnInit {
  private readonly calendarService = inject(CalendarService);
  private readonly gamesService = inject(GamesService);
  private readonly leaguesService = inject(LeaguesService);

  readonly eventos = signal<CalendarioEvento[]>([]);
  readonly juegos = signal<Juego[]>([]);
  readonly ligas = signal<League[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly juegoIdFiltro = signal<number | null>(null);
  readonly ligaIdFiltro = signal<number | null>(null);
  readonly desdeFiltro = signal('');
  readonly hastaFiltro = signal('');

  ngOnInit(): void {
    this.gamesService.listActivos().subscribe({ next: (j) => this.juegos.set(j) });
    this.leaguesService.list().subscribe({ next: (l) => this.ligas.set(l) });
    this.buscar();
  }

  buscar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.calendarService.consultar({
      juegoId: this.juegoIdFiltro(),
      ligaId: this.ligaIdFiltro(),
      desde: this.desdeFiltro() ? `${this.desdeFiltro()}T00:00:00` : null,
      hasta: this.hastaFiltro() ? `${this.hastaFiltro()}T23:59:59` : null
    }).subscribe({
      next: (eventos) => {
        this.eventos.set(eventos);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el calendario de eventos.');
        this.cargando.set(false);
      }
    });
  }

  limpiarFiltros(): void {
    this.juegoIdFiltro.set(null);
    this.ligaIdFiltro.set(null);
    this.desdeFiltro.set('');
    this.hastaFiltro.set('');
    this.buscar();
  }
}
