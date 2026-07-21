import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthService } from '../../../../core/services/auth.service';
import { TeamsService } from '../../../teams/services/teams.service';
import { TransfersService } from '../../../transfers/services/transfers.service';
import { LeaguesService } from '../../../leagues/services/leagues.service';
import { GamesService } from '../../../games/services/games.service';
import { Invitacion } from '../../../../models/invitacion.model';
import { Transferencia } from '../../../../models/transferencia.model';
import { League } from '../../../../models/league.model';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatCardComponent } from '../../../../shared/components/stat-card/stat-card.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';

/**
 * Panel principal.
 *
 * Muestra unicamente datos que el backend expone hoy: invitaciones
 * pendientes, transferencias y ligas. El diseno incluia ademas torneos
 * activos, proximos enfrentamientos y estadisticas de partidas, que
 * dependen de EPIC-07 y EPIC-13 y todavia no existen.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    DatePipe,
    PageHeaderComponent,
    StatCardComponent,
    EmptyStateComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly teamsService = inject(TeamsService);
  private readonly transfersService = inject(TransfersService);
  private readonly leaguesService = inject(LeaguesService);
  private readonly gamesService = inject(GamesService);
  readonly auth = inject(AuthService);

  readonly cargando = signal(true);
  readonly invitaciones = signal<Invitacion[]>([]);
  readonly transferencias = signal<Transferencia[]>([]);
  readonly ligas = signal<League[]>([]);
  readonly totalJuegos = signal(0);

  /** Si todas las peticiones fallan mostramos un error, no un panel en ceros. */
  readonly errorGeneral = signal(false);

  readonly hoy = new Date();

  readonly nombreCorto = computed(() => {
    const nombre = this.auth.usuario()?.nombre?.trim();
    return nombre ? nombre.split(/\s+/)[0] : null;
  });

  readonly saludo = computed(() => {
    const nombre = this.nombreCorto();
    return nombre ? `Hola de nuevo, ${nombre}` : 'Panel principal';
  });

  readonly sinPendientes = computed(
    () => this.invitaciones().length === 0 && this.transferencias().length === 0
  );

  /** Las 4 ligas mas recientes, por id descendente. */
  readonly ligasRecientes = computed(() =>
    [...this.ligas()].sort((a, b) => b.id - a.id).slice(0, 4)
  );

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorGeneral.set(false);

    // Cada fuente se degrada por separado: que falle una no debe vaciar
    // el panel entero.
    forkJoin({
      invitaciones: this.teamsService.misInvitacionesPendientes().pipe(catchError(() => of(null))),
      transferencias: this.transfersService.pendientes().pipe(catchError(() => of(null))),
      ligas: this.leaguesService.list().pipe(catchError(() => of(null))),
      juegos: this.gamesService.listActivos().pipe(catchError(() => of(null)))
    }).subscribe((res) => {
      this.invitaciones.set(res.invitaciones ?? []);
      this.transferencias.set(res.transferencias ?? []);
      this.ligas.set(res.ligas ?? []);
      this.totalJuegos.set(res.juegos?.length ?? 0);

      const todoFallo =
        res.invitaciones === null &&
        res.transferencias === null &&
        res.ligas === null &&
        res.juegos === null;
      this.errorGeneral.set(todoFallo);
      this.cargando.set(false);
    });
  }
}
