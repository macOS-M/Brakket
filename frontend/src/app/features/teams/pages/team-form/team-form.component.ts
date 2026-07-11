import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { Juego } from '../../../../models/juego.model';
import { GamesService } from '../../../games/services/games.service';
import { TeamsService } from '../../services/teams.service';

@Component({
  selector: 'app-team-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './team-form.component.html',
  styleUrl: './team-form.component.scss'
})
export class TeamFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly gamesService = inject(GamesService);
  private readonly teamsService = inject(TeamsService);
  private readonly router = inject(Router);

  readonly juegos = signal<Juego[]>([]);
  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    logo: [''],
    descripcion: ['', [Validators.maxLength(500)]],
    juegoId: [null as number | null, [Validators.required]],
    redesSociales: this.fb.nonNullable.array<string>([])
  });

  get redesSociales(): FormArray {
    return this.form.get('redesSociales') as FormArray;
  }

  ngOnInit(): void {
    this.gamesService.listActivos().subscribe({
      next: (juegos) => this.juegos.set(juegos),
      error: () => this.error.set('No se pudo cargar el catalogo de juegos.')
    });
  }

  agregarRedSocial(): void {
    this.redesSociales.push(this.fb.nonNullable.control('', [Validators.required]));
  }

  quitarRedSocial(index: number): void {
    this.redesSociales.removeAt(index);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.error.set(null);
    const valores = this.form.getRawValue();

    this.teamsService.crear({
      nombre: valores.nombre,
      logo: valores.logo || null,
      descripcion: valores.descripcion || null,
      juegoId: valores.juegoId!,
      redesSociales: valores.redesSociales
    }).subscribe({
      next: (equipo) => this.router.navigate(['/teams', equipo.id, 'plantilla']),
      error: (err) => {
        this.guardando.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo crear el equipo.');
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/teams']);
  }
}
