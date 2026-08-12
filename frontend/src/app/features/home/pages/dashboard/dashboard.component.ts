import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
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
import { Juego, JuegoExterno } from '../../../../models/juego.model';
import { Torneo } from '../../../../models/tournament.model';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { FechaRelativaPipe } from '../../../../shared/pipes/fecha-relativa.pipe';
import { AdSlotComponent } from '../../../../shared/components/ad-slot/ad-slot.component';
import { portadaFoto, portadaGradiente } from '../../../../shared/utils/cover';
import { ahoraCostaRica } from '../../../../shared/utils/hora-costa-rica';
import { RolEquipoPipe } from '../../../../shared/pipes/rol-equipo.pipe';

/**
 * Panel principal (referencia: dashboard de jugador): héroe con el juego
 * destacado, fila de juegos top y rail de próximos torneos, todo con datos
 * reales. Los pendientes accionables y la gestión por rol siguen debajo.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe, PageHeaderComponent, EmptyStateComponent, FechaRelativaPipe, AdSlotComponent, RolEquipoPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
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
  readonly misCompetencias = signal<Torneo[]>([]);

  /** Si todas las peticiones fallan mostramos un error, no un panel en ceros. */
  readonly errorGeneral = signal(false);

  /** Fecha del saludo: la de Costa Rica, no la del dispositivo de quien mira. */
  readonly hoy = ahoraCostaRica();

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

  /** Carousel del héroe: los juegos más jugados (rating RAWG), rotando. */
  readonly heroIndex = signal(0);
  readonly heroPausado = signal(false);
  private heroTimer: ReturnType<typeof setInterval> | null = null;

  readonly heroJuegos = computed(() =>
    [...this.juegos()]
      .filter((j) => j.imagenUrl)
      .sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0))
      .slice(0, 5));

  /** Índice acotado al largo real de la lista (para transform y puntos). */
  readonly heroVisible = computed(() => {
    const total = this.heroJuegos().length;
    return total > 0 ? this.heroIndex() % total : 0;
  });

  /** Fila de juegos top por popularidad real de RAWG (rating desc). */
  readonly mostrandoMas = signal(false);

  /** Top real de RAWG (no depende del catálogo local). */
  readonly topRawg = signal<JuegoExterno[]>([]);

  // Estable a propósito (sin carrusel). Si un top ya está en el catálogo,
  // el póster navega a su página; si no, al catálogo para importarlo.
  readonly topJuegos = computed(() => {
    const porNombre = new Map(this.juegos().map((j) => [j.nombre.toLowerCase(), j.id]));
    const lista = this.topRawg().length > 0
      ? this.topRawg().map((t) => ({
          nombre: t.nombre,
          imagenUrl: t.imagenUrl,
          idCatalogo: porNombre.get(t.nombre.toLowerCase()) ?? null
        }))
      : [...this.juegos()]
          .sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0))
          .map((j) => ({ nombre: j.nombre, imagenUrl: j.imagenUrl, idCatalogo: j.id }));
    return lista.slice(0, this.mostrandoMas() ? 18 : 6);
  });

  ngOnDestroy(): void {
    if (this.heroTimer) {
      clearInterval(this.heroTimer);
    }
  }

  /** Rail de próximos torneos (referencia "Upcoming Tournaments"): solo
   *  competencias con futuro (abiertas o en curso), las más cercanas primero. */
  readonly proximosTorneos = computed(() =>
    this.torneos()
      .filter((t) => t.estado === 'INSCRIPCION_ABIERTA' || t.estado === 'EN_CURSO')
      .sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio))
      .slice(0, 5));

  /**
   * "Tus competencias": lo accionable primero (referencia CM) — torneos en
   * curso arriba (hay resultados que reportar), luego los que vienen.
   */
  readonly competencias = computed(() => {
    const orden: Record<string, number> = { EN_CURSO: 0, INSCRIPCION_ABIERTA: 1 };
    return this.misCompetencias()
      .filter((t) => t.estado !== 'FINALIZADO' && t.estado !== 'CANCELADO')
      .sort((a, b) =>
        (orden[a.estado] ?? 2) - (orden[b.estado] ?? 2)
        || a.fechaInicio.localeCompare(b.fechaInicio))
      .slice(0, 4);
  });

  /** Sin nada organizado: se le ofrece convertirse en organizador (CM). */
  readonly organizaAlgo = computed(() => {
    const uid = Number(this.auth.usuario()?.id);
    return !!uid && this.misCompetencias().some((t) => t.organizadorId === uid);
  });

  esOrganizadorDe(torneo: Torneo): boolean {
    return Number(this.auth.usuario()?.id) === torneo.organizadorId;
  }

  badgeDe(torneo: Torneo): { texto: string; clase: string } {
    switch (torneo.estado) {
      case 'EN_CURSO':
        return { texto: '● En curso', clase: 'en-curso' };
      case 'FINALIZADO':
        return { texto: 'Finalizado', clase: 'neutro' };
      case 'CANCELADO':
        return { texto: 'Cancelado', clase: 'neutro' };
      default:
        return torneo.inscritos >= torneo.maxEquipos
          ? { texto: 'Cupo lleno', clase: 'ambar' }
          : { texto: 'Abierta', clase: 'verde' };
    }
  }

  ngOnInit(): void {
    this.cargar();
    // Rotación del héroe cada 3 s; se pausa con el cursor encima.
    this.heroTimer = setInterval(() => {
      if (!this.heroPausado() && this.heroJuegos().length > 1) {
        this.heroIndex.update((i) => (i + 1) % this.heroJuegos().length);
      }
    }, 3000);
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
      torneos: this.tournamentsService.listar().pipe(catchError(() => of(null))),
      topRawg: this.gamesService.topRawg().pipe(catchError(() => of([] as JuegoExterno[]))),
      misCompetencias: conSesion
        ? this.tournamentsService.misCompetencias().pipe(catchError(() => of(null)))
        : of([] as Torneo[])
    }).subscribe((res) => {
      this.invitaciones.set(res.invitaciones ?? []);
      this.transferencias.set(res.transferencias ?? []);
      this.ligas.set(res.ligas ?? []);
      this.juegos.set(res.juegos ?? []);
      this.torneos.set(res.torneos ?? []);
      this.topRawg.set(res.topRawg ?? []);
      this.misCompetencias.set(res.misCompetencias ?? []);

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
