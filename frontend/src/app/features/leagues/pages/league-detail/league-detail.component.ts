import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { League, Season, SeasonRequest } from '../../../../models/league.model';
import { LeaguesService } from '../../services/leagues.service';

/**
 * Detalle de una liga (RF-22): muestra sus datos, permite ir a configurarla y
 * gestiona sus temporadas (listar y agregar).
 */
@Component({
  selector: 'app-league-detail',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './league-detail.component.html',
  styleUrl: './league-detail.component.scss'
})
export class LeagueDetailComponent {
  private readonly fb = inject(FormBuilder);
  private readonly leaguesService = inject(LeaguesService);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  readonly league = signal<League | null>(null);
  readonly seasons = signal<Season[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly savingSeason = signal(false);
  readonly seasonError = signal<string | null>(null);

  /** Solo el comisionado ve las acciones de configuración (el backend igual las protege). */
  readonly esComisionado = computed(() => {
    const liga = this.league();
    const usuario = this.auth.usuario();
    return !!liga && !!usuario?.id && Number(usuario.id) === liga.comisionadoId;
  });

  private ligaId!: number;

  readonly seasonForm = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    fechaInicio: ['', [Validators.required]],
    fechaFin: ['', [Validators.required]]
  });

  constructor() {
    this.ligaId = Number(this.route.snapshot.paramMap.get('id'));
    this.cargarLiga();
    this.cargarTemporadas();
  }

  private cargarLiga(): void {
    this.leaguesService.getById(this.ligaId).subscribe({
      next: (liga) => {
        this.league.set(liga);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la liga.');
        this.loading.set(false);
      }
    });
  }

  private cargarTemporadas(): void {
    this.leaguesService.listSeasons(this.ligaId).subscribe({
      next: (temporadas) => this.seasons.set(temporadas),
      error: () => this.seasonError.set('No se pudieron cargar las temporadas.')
    });
  }

  agregarTemporada(): void {
    if (this.seasonForm.invalid) {
      this.seasonForm.markAllAsTouched();
      return;
    }
    this.savingSeason.set(true);
    this.seasonError.set(null);

    const body: SeasonRequest = {
      nombre: this.seasonForm.value.nombre!.trim(),
      fechaInicio: this.seasonForm.value.fechaInicio!,
      fechaFin: this.seasonForm.value.fechaFin!
    };

    this.leaguesService.createSeason(this.ligaId, body).subscribe({
      next: (temporada) => {
        this.seasons.update((actuales) => [...actuales, temporada]);
        this.seasonForm.reset();
        this.savingSeason.set(false);
      },
      error: (err) => {
        this.seasonError.set(err?.error?.message ?? 'No se pudo agregar la temporada.');
        this.savingSeason.set(false);
      }
    });
  }
}
