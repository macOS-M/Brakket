import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, map, of, switchMap, takeUntil } from 'rxjs';

import { EquipoBusqueda, Pagina } from '../../../../models/equipo.model';
import { Juego } from '../../../../models/juego.model';
import { GamesService } from '../../../games/services/games.service';
import { TeamsService } from '../../services/teams.service';

/** Largo máximo del texto de búsqueda (igual al nombre de equipo en la BD). */
const LARGO_MAXIMO_TEXTO = 120;
const TAMANO_PAGINA = 12;

/**
 * Búsqueda y listado de equipos (RF-05).
 */
@Component({
  selector: 'app-team-list',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './team-list.component.html',
  styleUrl: './team-list.component.scss'
})
export class TeamListComponent implements OnInit, OnDestroy {
  private readonly teamsService = inject(TeamsService);
  private readonly gamesService = inject(GamesService);
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

  ngOnInit(): void {
    this.cargarJuegos();

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
