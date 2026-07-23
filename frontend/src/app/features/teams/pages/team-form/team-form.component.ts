import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { Juego } from '../../../../models/juego.model';
import { GamesService } from '../../../games/services/games.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { FotoInputComponent } from '../../../../shared/components/foto-input/foto-input.component';
import { TeamsService } from '../../services/teams.service';

@Component({
  selector: 'app-team-form',
  standalone: true,
  imports: [ReactiveFormsModule, PageHeaderComponent, FotoInputComponent],
  templateUrl: './team-form.component.html',
  styleUrl: './team-form.component.scss'
})
export class TeamFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly gamesService = inject(GamesService);
  private readonly teamsService = inject(TeamsService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly juegos = signal<Juego[]>([]);
  readonly guardando = signal(false);
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  readonly exito = signal<string | null>(null);

  /** null = modo "crear"; con valor = modo "editar" ese equipo. */
  readonly equipoId = signal<number | null>(null);

  /** Versión del equipo leída en el GET; viaja en el PUT (concurrencia optimista). */
  private readonly version = signal<number | null>(null);

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    logo: [''],
    bannerUrl: [''],
    descripcion: ['', [Validators.maxLength(500)]],
    sitioWeb: [''],
    videoUrl: [''],
    juegoId: [null as number | null, [Validators.required]],
    estadoPrivacidad: ['PUBLIC'],
    redesSociales: this.fb.nonNullable.array<string>([])
  });

  get redesSociales(): FormArray {
    return this.form.get('redesSociales') as FormArray;
  }

  get esEdicion(): boolean {
    return this.equipoId() !== null;
  }

  ngOnInit(): void {
    this.gamesService.listActivos().subscribe({
      next: (juegos) => this.juegos.set(juegos),
      error: () => this.error.set('No se pudo cargar el catalogo de juegos.')
    });

    const idParam = this.route.snapshot.paramMap.get('equipoId');
    if (idParam) {
      const id = Number(idParam);
      this.equipoId.set(id);
      this.cargarEquipo(id);
    }
  }

  private cargarEquipo(id: number): void {
    this.cargando.set(true);
    this.teamsService.obtenerPorId(id).subscribe({
      next: (equipo) => {
        this.version.set(equipo.version);
        this.form.patchValue({
          nombre: equipo.nombre,
          logo: equipo.logo ?? '',
          bannerUrl: equipo.bannerUrl ?? '',
          descripcion: equipo.descripcion ?? '',
          sitioWeb: equipo.sitioWeb ?? '',
          videoUrl: equipo.videoUrl ?? '',
          juegoId: equipo.juegoId,
          estadoPrivacidad: equipo.estadoPrivacidad
        });
        this.redesSociales.clear();
        equipo.redesSociales.forEach((url) =>
          this.redesSociales.push(this.fb.nonNullable.control(url, [Validators.required]))
        );
        this.cargando.set(false);
      },
      error: (err) => {
        this.cargando.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo cargar la información del equipo.');
      }
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
    this.exito.set(null);
    const valores = this.form.getRawValue();

    if (this.esEdicion) {
      // logo/descripción viajan tal cual: el string vacío le indica al
      // backend que borre el campo (null significaría "no tocar").
      this.teamsService.editar(this.equipoId()!, {
        nombre: valores.nombre,
        logo: valores.logo.trim(),
        bannerUrl: valores.bannerUrl.trim(),
        descripcion: valores.descripcion.trim(),
        sitioWeb: valores.sitioWeb.trim(),
        videoUrl: valores.videoUrl.trim(),
        juegoId: valores.juegoId,
        estadoPrivacidad: valores.estadoPrivacidad,
        redesSociales: valores.redesSociales,
        version: this.version() ?? undefined
      }).subscribe({
        next: (equipo) => {
          this.version.set(equipo.version);
          this.guardando.set(false);
          this.exito.set('Cambios guardados correctamente.');
        },
        error: (err) => {
          this.guardando.set(false);
          this.error.set(err?.error?.message ?? 'No se pudo actualizar el equipo.');
        }
      });
      return;
    }

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
