import { Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Observable, Subject, forkJoin, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../core/services/auth.service';
import { GamesService } from '../../features/games/services/games.service';
import { LeaguesService } from '../../features/leagues/services/leagues.service';
import { NotificationsService } from '../../features/notifications/services/notifications.service';
import { TeamsService } from '../../features/teams/services/teams.service';
import { Juego } from '../../models/juego.model';
import { League } from '../../models/league.model';
import { Notificacion, TipoNotificacion } from '../../models/notificacion.model';
import { diaMes } from '../../shared/utils/formato-fecha';
import { EquipoResumenPublico } from '../../models/perfil-equipo-publico.model';
import { EtiquetaPipe } from '../../shared/pipes/etiqueta.pipe';

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
  imports: [RouterLink, EtiquetaPipe],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss'
})
export class TopbarComponent {
  private readonly authService = inject(AuthService);
  private readonly gamesService = inject(GamesService);
  private readonly leaguesService = inject(LeaguesService);
  private readonly teamsService = inject(TeamsService);
  private readonly notificationsService = inject(NotificationsService);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly usuario = this.authService.usuario;

  readonly consulta = signal('');
  readonly abierto = signal(false);
  readonly buscando = signal(false);
  readonly resultados = signal<ResultadosBusqueda | null>(null);
  readonly notificacionesNoLeidas = signal(0);
  readonly panelNotificacionesAbierto = signal(false);
  readonly cargandoNotificaciones = signal(false);
  readonly notificacionesRecientes = signal<Notificacion[]>([]);

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

  /** El rol de mayor jerarquía, no el primero que devuelva la API. */
  readonly rolPrincipal = computed(() => {
    const jerarquia = ['ADMIN', 'COMISIONADO', 'ARBITRO', 'PATROCINADOR', 'CAPITAN', 'JUGADOR'];
    const roles = this.usuario()?.roles ?? [];
    return jerarquia.find((rol) => roles.includes(rol)) ?? roles[0] ?? 'Jugador';
  });

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

    if (this.authService.isAuthenticated()) {
      this.actualizarContador();
      this.notificationsService.cambios$
        .pipe(takeUntilDestroyed())
        .subscribe(() => {
          this.actualizarContador();
          if (this.panelNotificacionesAbierto()) this.cargarNotificaciones();
        });
    }
  }

  private actualizarContador(): void {
    this.notificationsService.unreadCount()
      .pipe(catchError(() => of(0)))
      .subscribe((total) => this.notificacionesNoLeidas.set(total));
  }

  alternarNotificaciones(evento: Event): void {
    evento.stopPropagation();
    const abrir = !this.panelNotificacionesAbierto();
    this.panelNotificacionesAbierto.set(abrir);
    this.abierto.set(false);
    if (abrir) {
      this.actualizarContador();
      this.cargarNotificaciones();
    }
  }

  cerrarNotificaciones(): void {
    this.panelNotificacionesAbierto.set(false);
  }

  marcarNotificacionLeida(notificacion: Notificacion, evento: Event): void {
    evento.stopPropagation();
    if (!notificacion.leida) {
      this.notificationsService.markRead(notificacion.id).subscribe({
        next: (actualizada) => this.notificacionesRecientes.update((items) =>
          items.map((item) => item.id === actualizada.id ? actualizada : item)
        )
      });
    }
    this.cerrarNotificaciones();
    this.router.navigate(this.notificationsService.destination(notificacion));
  }

  etiquetaNotificacion(tipo: TipoNotificacion): string {
    if (tipo.startsWith('INVITACION') || tipo.startsWith('SOLICITUD_')) return 'Invitación';
    if (tipo.startsWith('TRANSFERENCIA')) return 'Transferencia';
    return ({
      RESULTADO: 'Resultado',
      DISPUTA: 'Disputa',
      CAMBIO_TORNEO: 'Torneo',
      TRANSMISION: 'Transmisión',
      ADMINISTRATIVA: 'Sistema',
      EXPULSION_EQUIPO: 'Equipo',
      CORRECCION: 'Corrección',
      INVITACION: 'Invitación',
      TRANSFERENCIA: 'Transferencia'
    } as Partial<Record<TipoNotificacion, string>>)[tipo] ?? 'Actividad';
  }

  fechaNotificacion(valor: string): string {
    const fecha = new Date(valor);
    const minutos = Math.max(0, Math.floor((Date.now() - fecha.getTime()) / 60_000));
    if (minutos < 1) return 'Ahora';
    if (minutos < 60) return `Hace ${minutos} min`;
    if (minutos < 1440) return `Hace ${Math.floor(minutos / 60)} h`;
    return diaMes(fecha);
  }

  private cargarNotificaciones(): void {
    this.cargandoNotificaciones.set(true);
    this.notificationsService.list(5)
      .pipe(
        catchError(() => of([] as Notificacion[])),
        tap(() => this.cargandoNotificaciones.set(false))
      )
      .subscribe((items) => this.notificacionesRecientes.set(items));
  }

  @HostListener('document:keydown.escape')
  alPresionarEscape(): void {
    this.cerrar();
    this.cerrarNotificaciones();
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
      this.cerrarNotificaciones();
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
