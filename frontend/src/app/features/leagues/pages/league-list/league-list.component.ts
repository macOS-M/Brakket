import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { League } from '../../../../models/league.model';
import { LeaguesService } from '../../services/leagues.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { portadaFoto, portadaGradiente } from '../../../../shared/utils/cover';

/**
 * Listado de ligas (RF-22). Punto de entrada de la feature: muestra las ligas
 * existentes y permite ir a crear una nueva o abrir el detalle de cada una.
 */
@Component({
  selector: 'app-league-list',
  standalone: true,
  imports: [RouterLink, PageHeaderComponent, EmptyStateComponent],
  templateUrl: './league-list.component.html',
  styleUrl: './league-list.component.scss'
})
export class LeagueListComponent {
  private readonly leaguesService = inject(LeaguesService);

  readonly leagues = signal<League[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    this.cargar();
  }

  /** La portada de la liga es la foto de su juego; sin foto, gradiente. */
  foto(juegoNombre: string | null): string | null {
    return juegoNombre ? portadaFoto(juegoNombre) : null;
  }

  /** Portada determinística por nombre de liga (ver shared/utils/cover). */
  portada(nombre: string): string {
    return portadaGradiente(nombre);
  }

  private cargar(): void {
    this.loading.set(true);
    this.error.set(null);
    this.leaguesService.list().subscribe({
      next: (ligas) => {
        this.leagues.set(ligas);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las ligas.');
        this.loading.set(false);
      }
    });
  }
}
