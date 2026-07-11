import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { GamesService } from '../../services/games.service';

/**
 * Formulario de creacion/edicion de juego (RF-20).
 */
@Component({
  selector: 'app-game-form',
  standalone: true,
  imports: [ReactiveFormsModule],
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

  private juegoId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    genero: ['', [Validators.required, Validators.maxLength(80)]],
    descripcion: ['', [Validators.maxLength(1000)]]
  });

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
          descripcion: juego.descripcion ?? ''
        });
      },
      error: () => this.error.set('No se pudo cargar el juego a editar.')
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.error.set(null);
    const request = this.form.getRawValue();

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
