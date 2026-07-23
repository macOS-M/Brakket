import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { EquipoElegible, TorneoDetalle } from '../../../../models/tournament.model';
import { TournamentsService } from '../../services/tournaments.service';
import { AuthService } from '../../../../core/services/auth.service';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { portadaFoto, portadaGradiente } from '../../../../shared/utils/cover';

type TabDetalle = 'resumen' | 'equipos';

/**
 * Detalle de torneo (RF-24/RF-25): banner con el arte del juego, tabs de
 * Resumen y Equipos (con las plantillas inscritas), inscripción del capitán
 * y eliminación para el organizador o un ADMIN.
 */
@Component({
  selector: 'app-tournament-detail',
  standalone: true,
  imports: [RouterLink, DatePipe, EmptyStateComponent],
  templateUrl: './tournament-detail.component.html',
  styleUrl: './tournament-detail.component.scss'
})
export class TournamentDetailComponent {
  private readonly tournamentsService = inject(TournamentsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly detalle = signal<TorneoDetalle | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly tab = signal<TabDetalle>('resumen');

  readonly elegibles = signal<EquipoElegible[]>([]);
  readonly equipoElegido = signal<number | null>(null);
  readonly inscribiendo = signal(false);
  readonly errorInscripcion = signal<string | null>(null);
  readonly inscripcionExitosa = signal(false);

  readonly confirmandoEliminar = signal(false);
  readonly eliminando = signal(false);
  readonly errorEliminar = signal<string | null>(null);

  private readonly torneoId: number;

  readonly torneo = computed(() => this.detalle()?.torneo ?? null);

  readonly arte = computed(() => {
    const t = this.torneo();
    return t ? t.juegoImagenUrl || portadaFoto(t.juegoNombre) : null;
  });

  readonly gradiente = computed(() => portadaGradiente(this.torneo()?.juegoNombre ?? '?'));

  readonly cupoLleno = computed(() => {
    const t = this.torneo();
    return !!t && t.inscritos >= t.maxEquipos;
  });

  readonly comenzo = computed(() => {
    const t = this.torneo();
    return !!t && new Date(t.fechaInicio) <= new Date();
  });

  readonly abierto = computed(() => {
    const t = this.torneo();
    return !!t && t.estado === 'ABIERTO' && !this.comenzo() && !this.cupoLleno();
  });

  readonly esOrganizador = computed(() => {
    const t = this.torneo();
    const usuario = this.auth.usuario();
    return !!t && !!usuario?.id && Number(usuario.id) === t.organizadorId;
  });

  readonly puedeEliminar = computed(
    () => this.esOrganizador() || this.auth.hasRole('ADMIN')
  );

  /** La ve un ADMIN sobre un torneo ajeno: es moderación, no gestión propia. */
  readonly esModeracion = computed(() => this.puedeEliminar() && !this.esOrganizador());

  constructor() {
    this.torneoId = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.tournamentsService.obtener(this.torneoId).subscribe({
      next: (detalle) => {
        this.detalle.set(detalle);
        this.cargando.set(false);
        this.cargarElegibles();
      },
      error: () => {
        this.error.set('No se pudo cargar el torneo (¿existe o es privado?).');
        this.cargando.set(false);
      }
    });
  }

  private cargarElegibles(): void {
    if (!this.auth.isAuthenticated() || !this.abierto()) {
      return;
    }
    this.tournamentsService.equiposElegibles(this.torneoId).subscribe({
      next: (equipos) => {
        this.elegibles.set(equipos);
        if (equipos.length === 1) {
          this.equipoElegido.set(equipos[0].id);
        }
      },
      error: () => this.elegibles.set([])
    });
  }

  inscribir(): void {
    const equipoId = this.equipoElegido();
    if (!equipoId) {
      return;
    }
    this.inscribiendo.set(true);
    this.errorInscripcion.set(null);
    this.tournamentsService.inscribir(this.torneoId, equipoId).subscribe({
      next: (detalle) => {
        this.detalle.set(detalle);
        this.inscribiendo.set(false);
        this.inscripcionExitosa.set(true);
        this.elegibles.update((lista) => lista.filter((e) => e.id !== equipoId));
        this.equipoElegido.set(null);
        this.tab.set('equipos');
      },
      error: (err) => {
        this.inscribiendo.set(false);
        this.errorInscripcion.set(err?.error?.message ?? 'No se pudo inscribir el equipo.');
      }
    });
  }

  eliminar(): void {
    this.eliminando.set(true);
    this.errorEliminar.set(null);
    this.tournamentsService.eliminar(this.torneoId).subscribe({
      next: () => this.router.navigate(['/tournaments']),
      error: (err) => {
        this.eliminando.set(false);
        this.confirmandoEliminar.set(false);
        this.errorEliminar.set(err?.error?.message ?? 'No se pudo eliminar el torneo.');
      }
    });
  }
}
