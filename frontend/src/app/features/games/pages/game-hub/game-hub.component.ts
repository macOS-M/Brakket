import {
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  signal,
  viewChild
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Juego } from '../../../../models/juego.model';
import { League } from '../../../../models/league.model';
import { Torneo } from '../../../../models/tournament.model';
import { GamesService } from '../../services/games.service';
import { LeaguesService } from '../../../leagues/services/leagues.service';
import { TournamentsService } from '../../../tournaments/services/tournaments.service';
import { AuthService } from '../../../../core/services/auth.service';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { TorneoCardComponent } from '../../../tournaments/components/torneo-card/torneo-card.component';
import { TournamentWizardComponent } from '../../../tournaments/components/tournament-wizard/tournament-wizard.component';
import { portadaFoto, portadaGradiente } from '../../../../shared/utils/cover';

type TabHub = 'descripcion' | 'torneos';

/**
 * Hub publico de un juego (referencia Challenger Mode): banner con el arte,
 * tabs de Resumen y Torneos, y las ligas donde se compite ese titulo.
 * Modelo abierto: cualquier usuario con sesion crea ligas y torneos desde
 * el menu "Crear"; la gestion del catalogo (editar, perfil competitivo,
 * desactivar) es solo de ADMIN.
 */
@Component({
  selector: 'app-game-hub',
  standalone: true,
  imports: [RouterLink, DatePipe, EmptyStateComponent, TorneoCardComponent, TournamentWizardComponent],
  templateUrl: './game-hub.component.html',
  styleUrl: './game-hub.component.scss'
})
export class GameHubComponent {
  private readonly gamesService = inject(GamesService);
  private readonly leaguesService = inject(LeaguesService);
  private readonly tournamentsService = inject(TournamentsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly auth = inject(AuthService);

  readonly juego = signal<Juego | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly tab = signal<TabHub>('descripcion');

  /** Torneo destacado de la descripción: el próximo del juego. */
  readonly destacado = computed(() => this.torneos()[0] ?? null);

  /** El resto de torneos recientes para la fila de tarjetas. */
  readonly otrosTorneos = computed(() => this.torneos().slice(1, 4));

  /** Ligas del catalogo cuyo juego es este (se filtra en el cliente). */
  readonly ligas = signal<League[]>([]);

  readonly torneos = signal<Torneo[]>([]);
  readonly cargandoTorneos = signal(false);

  readonly menuCrearAbierto = signal(false);
  readonly wizardAbierto = signal(false);

  /** Zona y disparador del menú "Crear" (patrón menu button de ARIA). */
  private readonly zonaCrear = viewChild<ElementRef<HTMLElement>>('zonaCrear');
  private readonly btnCrear = viewChild<ElementRef<HTMLButtonElement>>('btnCrear');
  private readonly menuCrear = viewChild<ElementRef<HTMLElement>>('menuCrear');

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
    // El menú "Crear" se cierra al clickear fuera de su zona.
    const alClickGlobal = (evento: MouseEvent) => {
      if (this.menuCrearAbierto() && !this.zonaCrear()?.nativeElement.contains(evento.target as Node)) {
        this.menuCrearAbierto.set(false);
      }
    };
    document.addEventListener('click', alClickGlobal);
    this.destroyRef.onDestroy(() => document.removeEventListener('click', alClickGlobal));

    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.gamesService.obtenerPorId(id).subscribe({
      next: (juego) => {
        this.juego.set(juego);
        this.cargando.set(false);
        this.cargarLigas(juego);
        this.cargarTorneos(juego.id);
      },
      error: () => {
        this.error.set('No se pudo cargar el juego.');
        this.cargando.set(false);
      }
    });
  }

  private cargarLigas(juego: Juego): void {
    this.leaguesService.list().subscribe({
      next: (ligas) => this.ligas.set(ligas.filter((liga) => liga.juegoId === juego.id)),
      error: () => this.ligas.set([])
    });
  }

  private cargarTorneos(juegoId: number): void {
    this.cargandoTorneos.set(true);
    this.tournamentsService.listar(juegoId).subscribe({
      next: (torneos) => {
        this.torneos.set(torneos);
        this.cargandoTorneos.set(false);
      },
      error: () => {
        this.torneos.set([]);
        this.cargandoTorneos.set(false);
      }
    });
  }

  portadaLiga(liga: League): string | null {
    return liga.fotoUrl || this.foto();
  }

  gradienteLiga(liga: League): string {
    return portadaGradiente(liga.nombre);
  }

  /** Abre/cierra el menú; al abrir, el foco pasa a la primera opción. */
  alternarMenuCrear(): void {
    const abrir = !this.menuCrearAbierto();
    this.menuCrearAbierto.set(abrir);
    if (abrir) {
      setTimeout(() => this.opcionesMenuCrear()[0]?.focus());
    }
  }

  /** Cierra el menú y devuelve el foco al disparador. */
  cerrarMenuCrear(): void {
    this.menuCrearAbierto.set(false);
    this.btnCrear()?.nativeElement.focus();
  }

  /** Flechas / Home / End / Escape / Tab, según el patrón menu de ARIA. */
  tecladoMenuCrear(evento: KeyboardEvent): void {
    const opciones = this.opcionesMenuCrear();
    if (opciones.length === 0) {
      return;
    }
    const actual = opciones.indexOf(document.activeElement as HTMLElement);
    switch (evento.key) {
      case 'ArrowDown':
        evento.preventDefault();
        opciones[(actual + 1) % opciones.length].focus();
        break;
      case 'ArrowUp':
        evento.preventDefault();
        opciones[(actual - 1 + opciones.length) % opciones.length].focus();
        break;
      case 'Home':
        evento.preventDefault();
        opciones[0].focus();
        break;
      case 'End':
        evento.preventDefault();
        opciones[opciones.length - 1].focus();
        break;
      case 'Escape':
        this.cerrarMenuCrear();
        break;
      case 'Tab':
        // Tab sale del menú: se cierra sin secuestrar el foco.
        this.menuCrearAbierto.set(false);
        break;
    }
  }

  private opcionesMenuCrear(): HTMLElement[] {
    const menu = this.menuCrear()?.nativeElement;
    return menu ? Array.from(menu.querySelectorAll<HTMLElement>('[role="menuitem"]')) : [];
  }

  abrirWizard(): void {
    this.menuCrearAbierto.set(false);
    this.wizardAbierto.set(true);
  }

  crearLiga(): void {
    this.menuCrearAbierto.set(false);
    this.router.navigate(['/leagues/nuevo'], {
      queryParams: { juegoId: this.juego()?.id }
    });
  }

  torneoCreado(torneo: Torneo): void {
    this.wizardAbierto.set(false);
    this.router.navigate(['/tournaments', torneo.id]);
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
