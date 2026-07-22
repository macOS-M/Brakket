import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Juego } from '../../../../models/juego.model';
import { League } from '../../../../models/league.model';
import { GamesService } from '../../services/games.service';
import { LeaguesService } from '../../../leagues/services/leagues.service';
import { AuthService } from '../../../../core/services/auth.service';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { portadaFoto, portadaGradiente } from '../../../../shared/utils/cover';

type TabHub = 'resumen' | 'torneos';

/**
 * Hub publico de un juego (referencia Challenger Mode): banner con el arte,
 * tabs de Resumen y Torneos, y las ligas donde se compite ese titulo. Las
 * acciones de gestion del catalogo (editar, perfil competitivo, desactivar)
 * viven aca y solo las ve quien administra (RF-20).
 */
@Component({
  selector: 'app-game-hub',
  standalone: true,
  imports: [RouterLink, EmptyStateComponent],
  templateUrl: './game-hub.component.html',
  styleUrl: './game-hub.component.scss'
})
export class GameHubComponent {
  private readonly gamesService = inject(GamesService);
  private readonly leaguesService = inject(LeaguesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly juego = signal<Juego | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly tab = signal<TabHub>('resumen');

  /** Ligas del catalogo cuyo juego es este (se filtra en el cliente). */
  readonly ligas = signal<League[]>([]);

  readonly confirmandoDesactivar = signal(false);
  readonly errorAccion = signal<string | null>(null);

  readonly foto = computed(() => {
    const juego = this.juego();
    if (!juego) {
      return null;
    }
    return juego.imagenUrl || portadaFoto(juego.nombre);
  });

  readonly gradiente = computed(() => portadaGradiente(this.juego()?.nombre ?? '?'));

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.gamesService.obtenerPorId(id).subscribe({
      next: (juego) => {
        this.juego.set(juego);
        this.cargando.set(false);
        this.cargarLigas(juego);
      },
      error: () => {
        this.error.set('No se pudo cargar el juego.');
        this.cargando.set(false);
      }
    });
  }

  private cargarLigas(juego: Juego): void {
    this.leaguesService.list().subscribe({
      next: (ligas) => this.ligas.set(ligas.filter((liga) => liga.juegoNombre === juego.nombre)),
      error: () => this.ligas.set([])
    });
  }

  portadaLiga(liga: League): string | null {
    return this.foto();
  }

  gradienteLiga(liga: League): string {
    return portadaGradiente(liga.nombre);
  }

  confirmarDesactivar(): void {
    const juego = this.juego();
    if (!juego) {
      return;
    }
    this.gamesService.desactivar(juego.id).subscribe({
      next: () => this.router.navigate(['/games']),
      error: (err) => {
        this.confirmandoDesactivar.set(false);
        this.errorAccion.set(err?.error?.message ?? 'No se pudo desactivar el juego.');
      }
    });
  }
}
