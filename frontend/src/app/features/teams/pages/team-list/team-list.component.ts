import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, startWith, switchMap, takeUntil, map, catchError, of } from 'rxjs';

import { EquipoResumenPublico } from '../../../../models/perfil-equipo-publico.model';
import { TeamsService } from '../../services/teams.service';

/**
 * Listado público de equipos (RF-04) con búsqueda por nombre.
 */
@Component({
  selector: 'app-team-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './team-list.component.html',
  styleUrl: './team-list.component.scss'
})
export class TeamListComponent implements OnInit, OnDestroy {
  private readonly teamsService = inject(TeamsService);
  private readonly destroy$ = new Subject<void>();
  /** Criterio tecleado; switchMap cancela el request anterior si llega otro. */
  private readonly criterio$ = new Subject<string>();

  readonly equipos = signal<EquipoResumenPublico[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.criterio$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        // La carga inicial va después del debounce para que no espere los 300ms.
        startWith(''),
        switchMap((criterio) => {
          this.cargando.set(true);
          this.error.set(null);
          return this.teamsService.listarPublicos(criterio).pipe(
            map((equipos) => ({ equipos, fallo: false })),
            catchError(() => of({ equipos: [] as EquipoResumenPublico[], fallo: true }))
          );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(({ equipos, fallo }) => {
        if (fallo) {
          this.error.set('No se pudieron cargar los equipos.');
        } else {
          this.equipos.set(equipos);
        }
        this.cargando.set(false);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  buscar(criterio: string): void {
    this.criterio$.next(criterio);
  }
}
