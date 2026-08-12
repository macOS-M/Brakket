import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { CompetitiveProfileService } from '../../../../core/services/competitive-profile.service';
import { CatalogoCompetitivo, PerfilCompetitivoRequest } from '../../../../models/perfil-competitivo.model';
import { GamesService } from '../../services/games.service';
import { FormatoTorneoPipe } from '../../../../shared/pipes/formato-torneo.pipe';

@Component({
  selector: 'app-competitive-profile-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, FormatoTorneoPipe],
  templateUrl: './competitive-profile-form.component.html',
  styleUrl: './competitive-profile-form.component.scss'
})
export class CompetitiveProfileFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly gamesService = inject(GamesService);
  private readonly profilesService = inject(CompetitiveProfileService);

  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);
  readonly confirmacion = signal<string | null>(null);
  readonly juegoNombre = signal('');
  readonly formatos = signal<CatalogoCompetitivo[]>([]);
  readonly estadisticas = signal<CatalogoCompetitivo[]>([]);
  readonly perfilId = signal<number | null>(null);
  readonly editando = computed(() => this.perfilId() !== null);

  private juegoId = 0;

  readonly form = this.fb.nonNullable.group({
    modalidad: ['EQUIPOS' as 'INDIVIDUAL' | 'EQUIPOS', Validators.required],
    plantillaMinima: [1, [Validators.required, Validators.min(1)]],
    plantillaMaxima: [1, [Validators.required, Validators.min(1)]],
    formatosIds: this.fb.nonNullable.control<number[]>([], Validators.required),
    estadisticasIds: this.fb.nonNullable.control<number[]>([], Validators.required)
  });

  ngOnInit(): void {
    this.juegoId = Number(this.route.snapshot.paramMap.get('juegoId'));
    if (!Number.isInteger(this.juegoId) || this.juegoId <= 0) {
      this.error.set('El identificador del juego no es válido.');
      this.cargando.set(false);
      return;
    }

    forkJoin({
      juego: this.gamesService.obtenerPorId(this.juegoId),
      formatos: this.profilesService.listarFormatos(),
      estadisticas: this.profilesService.listarEstadisticas(),
      perfil: this.profilesService.obtenerPorJuego(this.juegoId).pipe(catchError((err) => {
        if (err?.status === 404) return of(null);
        throw err;
      }))
    }).subscribe({
      next: ({ juego, formatos, estadisticas, perfil }) => {
        this.juegoNombre.set(juego.nombre);
        this.formatos.set(formatos);
        this.estadisticas.set(estadisticas);
        if (perfil) {
          this.perfilId.set(perfil.id);
          const estadisticasObligatorias = estadisticas
            .filter((item) => item.obligatorio)
            .map((item) => item.id);
          this.form.patchValue({
            modalidad: perfil.modalidad as 'INDIVIDUAL' | 'EQUIPOS',
            plantillaMinima: perfil.plantillaMinima,
            plantillaMaxima: perfil.plantillaMaxima,
            formatosIds: perfil.formatosIds,
            estadisticasIds: [...new Set([
              ...perfil.estadisticasIds,
              ...estadisticasObligatorias
            ])]
          });
        } else {
          this.form.controls.estadisticasIds.setValue(
            estadisticas.filter((item) => item.obligatorio).map((item) => item.id)
          );
        }
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la configuración competitiva.');
        this.cargando.set(false);
      }
    });

    this.form.controls.modalidad.valueChanges.subscribe((modalidad) => {
      if (modalidad === 'INDIVIDUAL') {
        this.form.patchValue({ plantillaMinima: 1, plantillaMaxima: 1 });
      }
    });
  }

  estaSeleccionado(campo: 'formatosIds' | 'estadisticasIds', id: number): boolean {
    return this.form.controls[campo].value.includes(id);
  }

  alternar(campo: 'formatosIds' | 'estadisticasIds', id: number, seleccionado: boolean): void {
    const control = this.form.controls[campo];
    const valores = control.value;
    control.setValue(seleccionado
      ? [...new Set([...valores, id])]
      : valores.filter((actual) => actual !== id));
    control.markAsTouched();
  }

  guardar(): void {
    this.error.set(null);
    this.confirmacion.set(null);
    const raw = this.form.getRawValue();
    if (this.form.invalid || raw.formatosIds.length === 0 || raw.estadisticasIds.length === 0) {
      this.form.markAllAsTouched();
      this.error.set('Completa los campos obligatorios y selecciona al menos un formato y una estadística.');
      return;
    }
    if (raw.plantillaMinima > raw.plantillaMaxima) {
      this.error.set('El tamaño mínimo no puede ser mayor al máximo.');
      return;
    }

    const request: PerfilCompetitivoRequest = { juegoId: this.juegoId, ...raw };
    this.guardando.set(true);
    const accion = this.perfilId()
      ? this.profilesService.actualizar(this.perfilId()!, request)
      : this.profilesService.crear(request);

    accion.subscribe({
      next: (perfil) => {
        this.perfilId.set(perfil.id);
        this.confirmacion.set(perfil.mensaje ?? 'Perfil competitivo guardado correctamente.');
        this.guardando.set(false);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo guardar el perfil competitivo.');
        this.guardando.set(false);
      }
    });
  }

  volver(): void {
    this.router.navigate(['/games']);
  }
}
