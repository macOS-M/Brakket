import { Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Observable, Subject, forkJoin, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../core/services/auth.service';
import { GamesService } from '../../features/games/services/games.service';
import { LeaguesService } from '../../features/leagues/services/leagues.service';
import { TeamsService } from '../../features/teams/services/teams.service';
import { Juego } from '../../models/juego.model';
import { League } from '../../models/league.model';
import { EquipoResumenPublico } from '../../models/perfil-equipo-publico.model';

interface ResultadosBusqueda {
  juegos: Juego[];
  ligas: League[];
  equipos: EquipoResumenPublico[];
}

/**
 * Barra superior del shell: buscador global, notificaciones y sesion.
 *
 * El buscador cruza las tres entidades con listado publico real (juegos,
 * ligas y equipos). Juegos y ligas se piden una sola vez y se filtran en
 * el cliente porque son catalogos chicos; equipos usa el endpoint publico
 * con criterio, que ya filtra en el servidor.
 */
@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss'
})
export class TopbarComponent {
  private readonly authService = inject(AuthService);
  private readonly gamesService = inject(GamesService);
  private readonly leaguesService = inject(LeaguesService);
  private readonly teamsService = inject(TeamsService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly usuario = this.authService.usuario;

  readonly consulta = signal('');
  readonly abierto = signal(false);
  readonly buscando = signal(false);
  readonly resultados = signal<ResultadosBusqueda | null>(null);

  private readonly consulta$ = new Subject<string>();
  private cacheJuegos: Juego[] | null = null;
  private cacheLigas: League[] | null = null;

  readonly iniciales = computed(() => {
    const nombre = this.usuario()?.nombre?.trim();
    if (!nombre) {
      return '?';
    }
    return nombre
      .split(/\s+/)
      .slice(0, 2)
      .map((parte) => parte.charAt(0).toUpperCase())
      .join('');
  });

  readonly rolPrincipal = computed(() => this.usuario()?.roles?.[0] || 'Jugador');

  readonly sinResultados = computed(() => {
    const res = this.resultados();
    return (
      res !== null &&
      res.juegos.length === 0 &&
      res.ligas.length === 0 &&
      res.equipos.length === 0
    );
  });

  constructor() {
    this.consulta$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((texto) => this.buscar(texto)),
        takeUntilDestroyed()
      )
      .subscribe((res) => {
        this.buscando.set(false);
        this.resultados.set(res);
        this.abierto.set(res !== null);
      });
  }

  alEscribir(valor: string): void {
    this.consulta.set(valor);
    if (valor.trim().length < 2) {
      this.abierto.set(false);
      this.resultados.set(null);
      this.buscando.set(false);
    } else {
      this.buscando.set(true);
    }
    this.consulta$.next(valor);
  }

  alEnfocar(): void {
    if (this.resultados() !== null && this.consulta().trim().length >= 2) {
      this.abierto.set(true);
    }
  }

  cerrar(): void {
    this.abierto.set(false);
  }

  /** Flecha abajo desde el input: pasa el foco al primer resultado. */
  enfocarPrimero(evento: Event): void {
    if (!this.abierto()) {
      return;
    }
    evento.preventDefault();
    const primero = this.host.nativeElement.querySelector<HTMLAnchorElement>('.resultado');
    primero?.focus();
  }

  @HostListener('document:click', ['$event'])
  alClickGlobal(evento: MouseEvent): void {
    if (!this.host.nativeElement.contains(evento.target as Node)) {
      this.cerrar();
    }
  }

  login(): void {
    this.authService.login();
  }

  logout(): void {
    this.authService.logout();
  }

  private buscar(texto: string): Observable<ResultadosBusqueda | null> {
    const q = texto.trim().toLowerCase();
    if (q.length < 2) {
      return of(null);
    }
    return forkJoin({
      juegos: this.juegosCacheados(),
      ligas: this.ligasCacheadas(),
      equipos: this.teamsService
        .listarPublicos(q)
        .pipe(catchError(() => of([] as EquipoResumenPublico[])))
    }).pipe(
      map(({ juegos, ligas, equipos }) => ({
        juegos: juegos.filter((j) => j.nombre.toLowerCase().includes(q)).slice(0, 4),
        ligas: ligas
          .filter(
            (l) =>
              l.nombre.toLowerCase().includes(q) ||
              (l.juegoNombre ?? '').toLowerCase().includes(q)
          )
          .slice(0, 4),
        equipos: equipos.slice(0, 4)
      }))
    );
  }

  private juegosCacheados(): Observable<Juego[]> {
    if (this.cacheJuegos) {
      return of(this.cacheJuegos);
    }
    return this.gamesService.listActivos().pipe(
      tap((juegos) => (this.cacheJuegos = juegos)),
      catchError(() => of([] as Juego[]))
    );
  }

  private ligasCacheadas(): Observable<League[]> {
    if (this.cacheLigas) {
      return of(this.cacheLigas);
    }
    return this.leaguesService.list().pipe(
      tap((ligas) => (this.cacheLigas = ligas)),
      catchError(() => of([] as League[]))
    );
  }
}
