import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import { Juego, JuegoExterno } from '../../../../models/juego.model';
import { GamesService } from '../../services/games.service';
import { AuthService } from '../../../../core/services/auth.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { portadaFoto, portadaGradiente } from '../../../../shared/utils/cover';

/**
 * Catalogo de juegos (RF-20), al estilo de la referencia Challenger Mode:
 * escaparate con dos filas en marquesina, buscador y grilla de portadas
 * altas. El catalogo se puebla solo desde RAWG (seeder del backend) y el
 * buscador tambien consulta la API en vivo: los titulos que faltan se
 * agregan con un clic, sin formulario. Cada tarjeta lleva al hub del
 * juego; las acciones de gestion viven alla.
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
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly juegos = signal<Juego[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly busqueda = signal('');
  readonly generoActivo = signal<string | null>(null);

  /** Resultados de RAWG que todavía no están en el catálogo local. */
  readonly externos = signal<JuegoExterno[]>([]);
  readonly buscandoExternos = signal(false);
  /** Nombre del juego externo que se está agregando (deshabilita su tarjeta). */
  readonly importando = signal<string | null>(null);
  readonly errorImportar = signal<string | null>(null);

  private readonly busquedaExterna$ = new Subject<string>();

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

  constructor() {
    // Búsqueda en vivo contra RAWG: lo que no está en el catálogo local
    // aparece igual y se agrega con un clic. Solo con sesión (el buscador
    // externo exige login para proteger la key).
    this.busquedaExterna$
      .pipe(
        debounceTime(350),
        distinctUntilChanged(),
        switchMap((texto) => {
          const q = texto.trim();
          if (q.length < 2 || !this.auth.isAuthenticated()) {
            return of([] as JuegoExterno[]);
          }
          this.buscandoExternos.set(true);
          return this.gamesService.buscarExterno(q).pipe(catchError(() => of([] as JuegoExterno[])));
        }),
        takeUntilDestroyed()
      )
      .subscribe((externos) => {
        this.buscandoExternos.set(false);
        const nombresLocales = new Set(this.juegos().map((j) => j.nombre.toLowerCase()));
        this.externos.set(externos.filter((e) => !nombresLocales.has(e.nombre.toLowerCase())));
      });
  }

  ngOnInit(): void {
    this.cargarJuegos();
  }

  alBuscar(valor: string): void {
    this.busqueda.set(valor);
    this.errorImportar.set(null);
    if (valor.trim().length < 2) {
      this.externos.set([]);
      this.buscandoExternos.set(false);
    }
    this.busquedaExterna$.next(valor);
  }

  /** Agrega un título de RAWG al catálogo y navega a su hub. */
  agregarExterno(externo: JuegoExterno): void {
    if (this.importando()) {
      return;
    }
    this.importando.set(externo.nombre);
    this.errorImportar.set(null);
    this.gamesService.importarExterno(externo.nombre).subscribe({
      next: (juego) => this.router.navigate(['/games', juego.id]),
      error: (err) => {
        this.importando.set(null);
        this.errorImportar.set(err?.error?.message ?? 'No se pudo agregar el juego.');
      }
    });
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
