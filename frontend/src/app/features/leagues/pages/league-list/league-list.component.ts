import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { League } from '../../../../models/league.model';
import { LeaguesService } from '../../services/leagues.service';

/**
 * Listado de ligas (RF-22). Punto de entrada de la feature: muestra las ligas
 * existentes y permite ir a crear una nueva o abrir el detalle de cada una.
 */
@Component({
  selector: 'app-league-list',
  standalone: true,
  imports: [RouterLink],
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
