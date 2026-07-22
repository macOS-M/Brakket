import { Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';

import { JuegoExterno } from '../../../../models/juego.model';
import { GamesService } from '../../services/games.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';

/**
 * Formulario de creacion/edicion de juego (RF-20).
 *
 * El alta no es manual: se busca el juego en el catalogo externo (RAWG, via
 * proxy del backend) y al elegir una tarjeta el formulario se precarga con
 * nombre, genero y arte oficial. Los campos quedan editables por si hay que
 * ajustar algo antes de guardar.
 */
@Component({
  selector: 'app-game-form',
  standalone: true,
  imports: [ReactiveFormsModule, PageHeaderComponent],
  templateUrl: './game-form.component.html',
  styleUrl: './game-form.component.scss'
})
export class GameFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly gamesService = inject(GamesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);
  readonly editando = signal(false);

  readonly buscando = signal(false);
  /** null = todavía no se buscó nada; [] = búsqueda sin coincidencias. */
  readonly resultados = signal<JuegoExterno[] | null>(null);
  readonly errorBusqueda = signal<string | null>(null);
  readonly consulta = signal('');

  private readonly consulta$ = new Subject<string>();
  private juegoId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    genero: ['', [Validators.required, Validators.maxLength(80)]],
    descripcion: ['', [Validators.maxLength(1000)]],
    imagenUrl: ['', [Validators.maxLength(500)]]
  });

  constructor() {
    this.consulta$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((texto) => {
          const q = texto.trim();
          if (q.length < 2) {
            return of(null);
          }
          this.buscando.set(true);
          this.errorBusqueda.set(null);
          return this.gamesService.buscarExterno(q).pipe(
            catchError((err) => {
              this.errorBusqueda.set(
                err?.error?.message ?? 'No se pudo consultar el catálogo externo.'
              );
              return of([] as JuegoExterno[]);
            })
          );
        }),
        tap(() => this.buscando.set(false)),
        takeUntilDestroyed()
      )
      .subscribe((res) => this.resultados.set(res));
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }
    this.juegoId = Number(idParam);
    this.editando.set(true);
    this.gamesService.obtenerPorId(this.juegoId).subscribe({
      next: (juego) => {
        this.form.patchValue({
          nombre: juego.nombre,
          genero: juego.genero,
          descripcion: juego.descripcion ?? '',
          imagenUrl: juego.imagenUrl ?? ''
        });
      },
      error: () => this.error.set('No se pudo cargar el juego a editar.')
    });
  }

  alBuscar(valor: string): void {
    this.consulta.set(valor);
    if (valor.trim().length < 2) {
      this.resultados.set(null);
      this.buscando.set(false);
    }
    this.consulta$.next(valor);
  }

  /** Precarga el formulario con el juego elegido del catálogo externo. */
  elegir(juego: JuegoExterno): void {
    this.form.patchValue({
      nombre: juego.nombre,
      genero: juego.genero,
      imagenUrl: juego.imagenUrl ?? ''
    });
    this.resultados.set(null);
    this.consulta.set('');
  }

  quitarImagen(): void {
    this.form.patchValue({ imagenUrl: '' });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.error.set(null);
    const valores = this.form.getRawValue();
    const request = {
      nombre: valores.nombre,
      genero: valores.genero,
      descripcion: valores.descripcion,
      imagenUrl: valores.imagenUrl.trim() || null
    };

    const accion = this.editando()
      ? this.gamesService.editar(this.juegoId!, request)
      : this.gamesService.crear(request);

    accion.subscribe({
      next: () => this.router.navigate(['/games']),
      error: (err) => {
        this.guardando.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo guardar el juego.');
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/games']);
  }
}
