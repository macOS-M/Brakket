import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, map, of, switchMap, takeUntil } from 'rxjs';

import { EquipoBusqueda, Pagina } from '../../../../models/equipo.model';
import { colorDeNombre, portadaGradiente } from '../../../../shared/utils/cover';
import { Juego } from '../../../../models/juego.model';
import { GamesService } from '../../../games/services/games.service';
import { TeamsService } from '../../services/teams.service';
import { AuthService } from '../../../../core/services/auth.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';

/** Largo máximo del texto de búsqueda (igual al nombre de equipo en la BD). */
const LARGO_MAXIMO_TEXTO = 120;
const TAMANO_PAGINA = 12;

/**
 * Búsqueda y listado de equipos (RF-05).
 */
@Component({
  selector: 'app-team-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    PageHeaderComponent,
    EmptyStateComponent,
    StatusBadgeComponent
  ],
  templateUrl: './team-list.component.html',
  styleUrl: './team-list.component.scss'
})
export class TeamListComponent implements OnInit, OnDestroy {
  private readonly teamsService = inject(TeamsService);
  private readonly gamesService = inject(GamesService);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();
  /** Página solicitada; switchMap cancela el request anterior si llega otro. */
  private readonly busqueda$ = new Subject<number>();

  readonly largoMaximoTexto = LARGO_MAXIMO_TEXTO;

  readonly filtros = this.fb.nonNullable.group({
    q: '',
    juegoId: '',
    disciplina: '',
    estado: ''
  });

  readonly pagina = signal<Pagina<EquipoBusqueda> | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly juegos = signal<Juego[]>([]);
  readonly disciplinas = signal<string[]>([]);

  /** Mis equipos primero; el resto de la búsqueda va aparte. */
  readonly misEquipos = signal<EquipoBusqueda[]>([]);
  private readonly misIds = computed(() => new Set(this.misEquipos().map((e) => e.id)));
  readonly otros = computed(() =>
    (this.pagina()?.items ?? []).filter((e) => !this.misIds().has(e.id)));

  /** Solicitudes enviadas en esta sesión, para no re-ofrecer el botón. */
  readonly solicitados = signal<Set<number>>(new Set());
  readonly solicitandoId = signal<number | null>(null);
  readonly avisoSolicitud = signal<string | null>(null);
  readonly errorSolicitud = signal<string | null>(null);

  ngOnInit(): void {
    this.cargarJuegos();
    if (this.auth.isAuthenticated()) {
      this.teamsService.misEquipos().subscribe({
        next: (equipos) => this.misEquipos.set(equipos),
        error: () => this.misEquipos.set([])
      });
    }

    // Un único stream de búsqueda: cada emisión cancela el request en vuelo,
    // así una respuesta vieja nunca pisa a una más reciente.
    this.busqueda$
      .pipe(
        switchMap((page) => {
          this.cargando.set(true);
          this.error.set(null);
          const { q, juegoId, disciplina, estado } = this.filtros.getRawValue();
          return this.teamsService
            .buscar({
              q: q.trim() || undefined,
              juegoId: juegoId ? Number(juegoId) : undefined,
              disciplina: disciplina || undefined,
              estado: estado || undefined,
              page,
              size: TAMANO_PAGINA
            })
            .pipe(
              map((pagina) => ({ pagina, fallo: false })),
              catchError(() => of({ pagina: null, fallo: true }))
            );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(({ pagina, fallo }) => {
        if (fallo) {
          this.error.set('No se pudo realizar la búsqueda de equipos.');
        } else {
          this.pagina.set(pagina);
        }
        this.cargando.set(false);
      });

    this.buscar(0);
    this.filtros.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntil(this.destroy$)
      )
      .subscribe(() => this.buscar(0));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  buscar(page: number): void {
    this.busqueda$.next(page);
  }

  reintentar(): void {
    this.buscar(this.pagina()?.page ?? 0);
  }

  limpiarFiltros(): void {
    this.filtros.setValue({ q: '', juegoId: '', disciplina: '', estado: '' });
  }

  irAPagina(page: number): void {
    const total = this.pagina()?.totalPages ?? 0;
    if (page < 0 || page >= total) {
      return;
    }
    this.buscar(page);
  }

  /** Banda de portada: gradiente determinístico del juego (o del nombre si no hay juego). */
  portada(equipo: EquipoBusqueda): string {
    return portadaGradiente(equipo.juegoNombre ?? equipo.nombre);
  }

  /** Color estable para el monograma de equipos sin logo. */
  colorDe(nombre: string): string {
    return colorDeNombre(nombre);
  }

  /** Toda la tarjeta navega: los míos a la plantilla, el resto al perfil. */
  abrir(equipo: EquipoBusqueda, esMio: boolean): void {
    this.router.navigate(esMio ? ['/teams', equipo.id, 'plantilla'] : ['/team-profile', equipo.id]);
  }

  puedeSolicitar(equipo: EquipoBusqueda): boolean {
    return this.auth.isAuthenticated()
      && equipo.estado === 'ACTIVO'
      && !this.misIds().has(equipo.id)
      && !this.solicitados().has(equipo.id);
  }

  solicitarUnion(equipo: EquipoBusqueda, event: Event): void {
    event.stopPropagation();
    this.solicitandoId.set(equipo.id);
    this.avisoSolicitud.set(null);
    this.errorSolicitud.set(null);
    this.teamsService.solicitarUnion(equipo.id, null).subscribe({
      next: () => {
        this.solicitandoId.set(null);
        this.solicitados.update((set) => new Set(set).add(equipo.id));
        this.avisoSolicitud.set(
          `Solicitud enviada a ${equipo.nombre}: su capitán la va a revisar.`);
      },
      error: (err) => {
        this.solicitandoId.set(null);
        this.errorSolicitud.set(err?.error?.message ?? 'No se pudo enviar la solicitud.');
      }
    });
  }

  private cargarJuegos(): void {
    this.gamesService.listActivos().subscribe({
      next: (juegos) => {
        this.juegos.set(juegos);
        this.disciplinas.set([...new Set(juegos.map((j) => j.genero))].sort());
      },
      error: () => {
        // Sin catálogo solo se pierden los combos de juego/disciplina;
        // la búsqueda por texto sigue funcionando.
      }
    });
  }
}
