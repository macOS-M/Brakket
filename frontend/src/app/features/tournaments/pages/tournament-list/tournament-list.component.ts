import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Torneo } from '../../../../models/tournament.model';
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

  readonly filtrados = computed(() => {
    const tamano = this.tamanoActivo();
    return this.torneos().filter((t) => tamano === null || t.tamanoEquipo === tamano);
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

  readonly tamanos = [1, 2, 3, 4, 5];

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
}
