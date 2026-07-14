import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PerfilEquipoPublico } from '../../../../models/perfil-equipo-publico.model';
import { TeamsService } from '../../services/teams.service';

/**
 * Equipos. Placeholder de la feature "teams".
 * Pendiente EPIC-04.
 */
@Component({
  selector: 'app-team-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './team-list.component.html',
  styleUrl: './team-list.component.scss'
})
export class TeamListComponent implements OnInit {
  private readonly teamsService = inject(TeamsService);
  readonly equipos = signal<PerfilEquipoPublico[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void { this.buscar(); }

  buscar(criterio = ''): void {
    this.cargando.set(true);
    this.teamsService.listarPublicos(criterio).subscribe({
      next: equipos => { this.equipos.set(equipos); this.cargando.set(false); },
      error: () => { this.error.set('No se pudieron cargar los equipos.'); this.cargando.set(false); }
    });
  }
}
