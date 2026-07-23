import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { PerfilEquipoPublico } from '../../../../models/perfil-equipo-publico.model';
import { TeamsService } from '../../services/teams.service';
import { StatCardComponent } from '../../../../shared/components/stat-card/stat-card.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-team-public-profile',
  standalone: true,
  imports: [DatePipe, RouterLink, StatCardComponent, StatusBadgeComponent, EmptyStateComponent],
  templateUrl: './team-public-profile.component.html',
  styleUrl: './team-public-profile.component.scss'
})
export class TeamPublicProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly teamsService = inject(TeamsService);

  readonly perfil = signal<PerfilEquipoPublico | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  /** Partidas totales; el backend solo entrega victorias y derrotas. */
  readonly partidas = computed(() => {
    const stats = this.perfil()?.estadisticas;
    return stats ? stats.victorias + stats.derrotas : 0;
  });

  readonly winrate = computed(() => {
    const stats = this.perfil()?.estadisticas;
    const total = this.partidas();
    if (!stats || !total) {
      return '—';
    }
    return `${Math.round((stats.victorias / total) * 100)}%`;
  });

  ngOnInit(): void {
    const equipoId = Number(this.route.snapshot.paramMap.get('equipoId'));
    if (!Number.isInteger(equipoId) || equipoId <= 0) {
      this.error.set('El identificador del equipo no es válido.');
      this.cargando.set(false);
      return;
    }
    this.teamsService.consultarPerfilPublico(equipoId).subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(
          err?.status === 404 ? 'Equipo no encontrado.' : 'No se pudo cargar el perfil del equipo.'
        );
        this.cargando.set(false);
      }
    });
  }

  iniciales(nombre: string): string {
    return nombre
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0])
      .join('')
      .toUpperCase();
  }

  /** Muestra el dominio en vez de la URL cruda, que desborda el contenedor. */
  etiquetaEnlace(url: string): string {
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      return url;
    }
  }
}
