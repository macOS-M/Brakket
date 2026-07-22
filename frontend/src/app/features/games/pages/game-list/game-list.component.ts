import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Juego } from '../../../../models/juego.model';
import { GamesService } from '../../services/games.service';
import { AuthService } from '../../../../core/services/auth.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { portadaFoto, portadaGradiente } from '../../../../shared/utils/cover';

/**
 * Catalogo de juegos (RF-20), al estilo de la referencia Challenger Mode:
 * escaparate con dos filas en marquesina, buscador y grilla de portadas
 * altas. Cada tarjeta lleva al hub del juego; las acciones de gestion
 * (editar, perfil competitivo, desactivar) viven alla.
 */
@Component({
  selector: 'app-game-list',
  standalone: true,
  imports: [RouterLink, PageHeaderComponent, EmptyStateComponent],
  templateUrl: './game-list.component.html',
  styleUrl: './game-list.component.scss'
})
export class GameListComponent implements OnInit {
  private readonly gamesService = inject(GamesService);
  readonly auth = inject(AuthService);

  readonly juegos = signal<Juego[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly busqueda = signal('');
  readonly generoActivo = signal<string | null>(null);

  /** Generos presentes en el catalogo, para armar los chips de filtro. */
  readonly generos = computed(() => {
    const vistos = new Set<string>();
    for (const juego of this.juegos()) {
      if (juego.genero?.trim()) {
        vistos.add(juego.genero.trim());
      }
    }
    return [...vistos].sort((a, b) => a.localeCompare(b));
  });

  readonly juegosFiltrados = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    const genero = this.generoActivo();
    return this.juegos().filter((juego) => {
      const calzaTexto = !texto || juego.nombre.toLowerCase().includes(texto);
      const calzaGenero = !genero || juego.genero?.trim() === genero;
      return calzaTexto && calzaGenero;
    });
  });

  readonly hayFiltros = computed(() => !!this.busqueda().trim() || this.generoActivo() !== null);

  /**
   * La marquesina duplica el catalogo para que el loop de la animacion sea
   * continuo (el keyframe corre hasta -50% y vuelve sin salto). Solo tiene
   * sentido con suficientes tarjetas; por debajo del minimo no se muestra.
   */
  readonly marquesina = computed(() => {
    const juegos = this.juegos();
    if (juegos.length < 4) {
      return null;
    }
    return [...juegos, ...juegos];
  });

  ngOnInit(): void {
    this.cargarJuegos();
  }

  cargarJuegos(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.gamesService.listActivos().subscribe({
      next: (juegos) => {
        this.juegos.set(juegos);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el catalogo de juegos.');
        this.cargando.set(false);
      }
    });
  }

  filtrarPorGenero(genero: string | null): void {
    this.generoActivo.set(this.generoActivo() === genero ? null : genero);
  }

  limpiarFiltros(): void {
    this.busqueda.set('');
    this.generoActivo.set(null);
  }

  /** Inicial del juego, para la portada cuando no hay imagen. */
  inicial(juego: Juego): string {
    return juego.nombre?.charAt(0).toUpperCase() ?? '?';
  }

  /** Foto de portada: imagen real del juego o foto de stock mapeada. */
  foto(juego: Juego): string | null {
    return juego.imagenUrl || portadaFoto(juego.nombre);
  }

  /** Portada determinística: mismo juego, mismo gradiente, siempre. */
  portada(juego: Juego): string {
    return portadaGradiente(juego.nombre);
  }
}
