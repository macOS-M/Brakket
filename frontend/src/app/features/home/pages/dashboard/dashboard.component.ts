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
import { TournamentsService } from '../../../tournaments/services/tournaments.service';
import { Invitacion } from '../../../../models/invitacion.model';
import { Transferencia } from '../../../../models/transferencia.model';
import { League } from '../../../../models/league.model';
import { Juego } from '../../../../models/juego.model';
import { Torneo } from '../../../../models/tournament.model';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { portadaFoto, portadaGradiente } from '../../../../shared/utils/cover';

/**
 * Panel principal (referencia: dashboard de jugador): héroe con el juego
 * destacado, fila de juegos top y rail de próximos torneos, todo con datos
 * reales. Los pendientes accionables y la gestión por rol siguen debajo.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe, PageHeaderComponent, EmptyStateComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly teamsService = inject(TeamsService);
  private readonly transfersService = inject(TransfersService);
  private readonly leaguesService = inject(LeaguesService);
  private readonly gamesService = inject(GamesService);
  private readonly tournamentsService = inject(TournamentsService);
  readonly auth = inject(AuthService);

  readonly cargando = signal(true);
  readonly invitaciones = signal<Invitacion[]>([]);
  readonly transferencias = signal<Transferencia[]>([]);
  readonly ligas = signal<League[]>([]);
  readonly juegos = signal<Juego[]>([]);
  readonly torneos = signal<Torneo[]>([]);

  /** Si todas las peticiones fallan mostramos un error, no un panel en ceros. */
  readonly errorGeneral = signal(false);

  readonly hoy = new Date();

  readonly nombreCorto = computed(() => {
    const nombre = this.auth.usuario()?.nombre?.trim();
    return nombre ? nombre.split(/\s+/)[0] : null;
  });

  readonly saludo = computed(() => {
    const nombre = this.nombreCorto();
    return nombre ? `Hola de nuevo, ${nombre}` : 'Bienvenido a Brakket';
  });

  readonly sinPendientes = computed(
    () => this.invitaciones().length === 0 && this.transferencias().length === 0
  );

  /** Héroe: el primer juego con arte del catálogo. */
  readonly heroJuego = computed(() => {
    const juegos = this.juegos();
    return juegos.find((j) => j.imagenUrl) ?? juegos[0] ?? null;
  });

  /** Fila de juegos top (excluye el héroe para no repetirlo). */
  readonly topJuegos = computed(() => {
    const hero = this.heroJuego();
    return this.juegos()
      .filter((j) => j.id !== hero?.id)
      .slice(0, 6);
  });

  /** Rail de próximos torneos (referencia "Upcoming Tournaments"). */
  readonly proximosTorneos = computed(() => this.torneos().slice(0, 5));

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorGeneral.set(false);

    // Sin sesión solo se piden las fuentes públicas: pedir invitaciones o
    // transferencias devolvería 401 y el interceptor expulsaría al login.
    const conSesion = this.auth.isAuthenticated();

    forkJoin({
      invitaciones: conSesion
        ? this.teamsService.misInvitacionesPendientes().pipe(catchError(() => of(null)))
        : of([] as Invitacion[]),
      transferencias: conSesion
        ? this.transfersService.pendientes().pipe(catchError(() => of(null)))
        : of([] as Transferencia[]),
      ligas: this.leaguesService.list().pipe(catchError(() => of(null))),
      juegos: this.gamesService.listActivos().pipe(catchError(() => of(null))),
      torneos: this.tournamentsService.listar().pipe(catchError(() => of(null)))
    }).subscribe((res) => {
      this.invitaciones.set(res.invitaciones ?? []);
      this.transferencias.set(res.transferencias ?? []);
      this.ligas.set(res.ligas ?? []);
      this.juegos.set(res.juegos ?? []);
      this.torneos.set(res.torneos ?? []);

      const todoFallo =
        res.invitaciones === null &&
        res.transferencias === null &&
        res.ligas === null &&
        res.juegos === null &&
        res.torneos === null;
      this.errorGeneral.set(todoFallo);
      this.cargando.set(false);
    });
  }

  foto(juego: Juego): string | null {
    return juego.imagenUrl || portadaFoto(juego.nombre);
  }

  gradiente(nombre: string): string {
    return portadaGradiente(nombre);
  }

  fotoTorneo(torneo: Torneo): string | null {
    return torneo.juegoImagenUrl || portadaFoto(torneo.juegoNombre);
  }
}
