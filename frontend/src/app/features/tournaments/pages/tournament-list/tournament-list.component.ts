import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Torneo } from '../../../../models/tournament.model';
import { ahoraCostaRica } from '../../../../shared/utils/hora-costa-rica';
import { TournamentsService } from '../../services/tournaments.service';
import { AuthService } from '../../../../core/services/auth.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { TorneoCardComponent } from '../../components/torneo-card/torneo-card.component';

/**
 * Listado global de torneos (RF-24, modelo abierto): los públicos de todos
 * los juegos, más los propios del usuario cuando hay sesión. Se crea desde
 * la página de cada juego (el torneo siempre nace atado a un juego).
 */
@Component({
  selector: 'app-tournament-list',
  standalone: true,
  imports: [RouterLink, PageHeaderComponent, EmptyStateComponent, TorneoCardComponent],
  templateUrl: './tournament-list.component.html',
  styleUrl: './tournament-list.component.scss'
})
export class TournamentListComponent implements OnInit {
  private readonly tournamentsService = inject(TournamentsService);
  readonly auth = inject(AuthService);

  readonly torneos = signal<Torneo[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  /** Filtro por tamaño de equipo (el 1v1…5v5 de la referencia). */
  readonly tamanoActivo = signal<number | null>(null);
  readonly tamanos = [1, 2, 3, 4, 5];

  /** Filtro por estado. Default: comenzados (ya arrancaron, no terminaron). */
  readonly estadoActivo = signal<string>('comenzados');
  readonly estadosFiltro = [
    { key: 'comenzados', label: 'Comenzados' },
    { key: 'abiertos', label: 'Abiertos' },
    { key: 'en_curso', label: 'En curso' },
    { key: 'finalizados', label: 'Finalizados' },
    { key: 'todos', label: 'Todos' }
  ];

  /** Filtro por juego (los presentes en la lista). */
  readonly juegoActivo = signal<number | null>(null);
  readonly juegos = computed(() => {
    const porId = new Map<number, string>();
    this.torneos().forEach((t) => porId.set(t.juegoId, t.juegoNombre));
    return Array.from(porId, ([id, nombre]) => ({ id, nombre }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre));
  });

  /** "Comenzó": inscripción abierta pero con la fecha de inicio ya pasada. */
  private comenzo(t: Torneo): boolean {
    return new Date(t.fechaInicio) <= ahoraCostaRica();
  }

  /** Abierto de verdad: inscripción abierta y fecha aún futura. */
  private esAbierto(t: Torneo): boolean {
    return t.estado === 'INSCRIPCION_ABIERTA' && !this.comenzo(t);
  }

  /** Orden: los abiertos arriba de todo; el resto conserva su orden por fecha. */
  private prioridad(t: Torneo): number {
    return this.esAbierto(t) ? 0 : 1;
  }

  readonly filtrados = computed(() => {
    const tamano = this.tamanoActivo();
    const estado = this.estadoActivo();
    const juego = this.juegoActivo();
    return this.torneos()
      .filter((t) => tamano === null || t.tamanoEquipo === tamano)
      .filter((t) => juego === null || t.juegoId === juego)
      .filter((t) => {
        switch (estado) {
          case 'todos':
            return true;
          case 'abiertos':
            return this.esAbierto(t);
          case 'en_curso':
            return t.estado === 'EN_CURSO';
          case 'finalizados':
            return t.estado === 'FINALIZADO';
          default: // 'comenzados': inscripción abierta pero con la fecha ya pasada
            return t.estado === 'INSCRIPCION_ABIERTA' && this.comenzo(t);
        }
      })
      .sort((a, b) => this.prioridad(a) - this.prioridad(b));
  });

  /** Mis torneos (los organizo yo) primero; el resto aparte. */
  readonly mios = computed(() => {
    const uid = Number(this.auth.usuario()?.id);
    return uid ? this.filtrados().filter((t) => t.organizadorId === uid) : [];
  });

  readonly otros = computed(() => {
    const idsMios = new Set(this.mios().map((t) => t.id));
    return this.filtrados().filter((t) => !idsMios.has(t.id));
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.tournamentsService.listar().subscribe({
      next: (torneos) => {
        this.torneos.set(torneos);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los torneos.');
        this.cargando.set(false);
      }
    });
  }

  filtrar(tamano: number | null): void {
    this.tamanoActivo.set(this.tamanoActivo() === tamano ? null : tamano);
  }

  filtrarEstado(key: string): void {
    this.estadoActivo.set(key);
  }

  filtrarJuego(id: number | null): void {
    this.juegoActivo.set(id);
  }

  limpiarFiltros(): void {
    this.estadoActivo.set('todos');
    this.tamanoActivo.set(null);
    this.juegoActivo.set(null);
  }
}
