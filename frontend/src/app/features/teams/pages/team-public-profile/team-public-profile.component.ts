import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { PerfilEquipoPublico } from '../../../../models/perfil-equipo-publico.model';
import { TeamsService } from '../../services/teams.service';

@Component({
  selector: 'app-team-public-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './team-public-profile.component.html',
  styleUrl: './team-public-profile.component.scss'
})
export class TeamPublicProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly teamsService = inject(TeamsService);

  readonly perfil = signal<PerfilEquipoPublico | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const equipoId = Number(this.route.snapshot.paramMap.get('equipoId'));
    if (!Number.isInteger(equipoId) || equipoId <= 0) {
      this.error.set('El identificador del equipo no es válido.');
      this.cargando.set(false);
      return;
    }
    this.teamsService.consultarPerfilPublico(equipoId).subscribe({
      next: (perfil) => { this.perfil.set(perfil); this.cargando.set(false); },
      error: (err) => {
        this.error.set(err?.status === 404 ? 'Equipo no encontrado.' : 'No se pudo cargar el perfil del equipo.');
        this.cargando.set(false);
      }
    });
  }

  iniciales(nombre: string): string {
    return nombre.split(' ').filter(Boolean).slice(0, 2).map((parte) => parte[0]).join('').toUpperCase();
  }

  porcentajeVictorias(victorias: number, derrotas: number): string {
    const total = victorias + derrotas;
    return total ? `${Math.round((victorias / total) * 100)}%` : '—';
  }
}
