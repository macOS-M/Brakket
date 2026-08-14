import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { PerfilEquipoPublico } from '../../../../models/perfil-equipo-publico.model';
import { TeamsService } from '../../services/teams.service';
import { AuthService } from '../../../../core/services/auth.service';
import { StatCardComponent } from '../../../../shared/components/stat-card/stat-card.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { RolEquipoPipe } from '../../../../shared/pipes/rol-equipo.pipe';
import { EtiquetaPipe } from '../../../../shared/pipes/etiqueta.pipe';

type TabPerfil = 'resumen' | 'miembros' | 'estadisticas';

@Component({
  selector: 'app-team-public-profile',
  standalone: true,
  imports: [DatePipe, RouterLink, StatCardComponent, StatusBadgeComponent, EmptyStateComponent, RolEquipoPipe, EtiquetaPipe],
  templateUrl: './team-public-profile.component.html',
  styleUrl: './team-public-profile.component.scss'
})
export class TeamPublicProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly teamsService = inject(TeamsService);
  readonly auth = inject(AuthService);

  readonly perfil = signal<PerfilEquipoPublico | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  /** Tabs del perfil (referencia Challenger Mode). */
  readonly tab = signal<TabPerfil>('resumen');

  /**
   * La capitanía vigente vive en la plantilla (RF-09 permite transferirla);
   * el capitán ve Invitar y Ajustes directo desde el perfil.
   */
  readonly esCapitan = computed(() => {
    const perfil = this.perfil();
    const uid = Number(this.auth.usuario()?.id);
    return !!perfil && !!uid
      && perfil.plantilla.some((m) => m.usuarioId === uid && m.rol === 'CAPITAN');
  });

  /** El ADMIN modera cualquier equipo: ve Ajustes y gestión sin ser miembro. */
  readonly esAdmin = computed(() => this.auth.hasRole('ADMIN'));

  /** Quién puede gestionar el equipo desde este perfil. */
  readonly puedeGestionar = computed(() => this.esCapitan() || this.esAdmin());

  /** Le falta identidad al equipo: el capitán ve la tarjeta de setup. */
  readonly perfilIncompleto = computed(() => {
    const perfil = this.perfil();
    return !!perfil && (!perfil.logo || !perfil.bannerUrl || !perfil.descripcion);
  });

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
