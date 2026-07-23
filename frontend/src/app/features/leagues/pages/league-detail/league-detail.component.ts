import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { League, Season, SeasonRequest } from '../../../../models/league.model';
import { Torneo } from '../../../../models/tournament.model';
import { LeaguesService } from '../../services/leagues.service';
import { TournamentsService } from '../../../tournaments/services/tournaments.service';
import { TorneoCardComponent } from '../../../tournaments/components/torneo-card/torneo-card.component';
import { TournamentWizardComponent } from '../../../tournaments/components/tournament-wizard/tournament-wizard.component';
import { portadaGradiente } from '../../../../shared/utils/cover';

/**
 * Detalle de una liga (RF-22): muestra sus datos, permite ir a configurarla y
 * gestiona sus temporadas (listar y agregar).
 */
@Component({
  selector: 'app-league-detail',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, DatePipe, TorneoCardComponent, TournamentWizardComponent],
  templateUrl: './league-detail.component.html',
  styleUrl: './league-detail.component.scss'
})
export class LeagueDetailComponent {
  private readonly fb = inject(FormBuilder);
  private readonly leaguesService = inject(LeaguesService);
  private readonly tournamentsService = inject(TournamentsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
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

  /** Eliminar la liga: su comisionado, o un ADMIN sobre cualquier liga. */
  readonly puedeEliminar = computed(() => this.esComisionado() || this.auth.hasRole('ADMIN'));

  /** La ve un ADMIN sobre una liga ajena: es moderación, no gestión propia. */
  readonly esModeracion = computed(() => this.puedeEliminar() && !this.esComisionado());

  /** Portada: foto propia de la liga o, en su defecto, el arte del juego. */
  readonly portada = computed(() => {
    const liga = this.league();
    return liga ? liga.fotoUrl || liga.juegoImagenUrl : null;
  });

  readonly gradiente = computed(() => portadaGradiente(this.league()?.nombre ?? '?'));

  readonly confirmandoEliminar = signal(false);
  readonly eliminando = signal(false);
  readonly errorEliminar = signal<string | null>(null);

  /** Torneos hospedados en esta liga (RF-24). */
  readonly torneos = signal<Torneo[]>([]);
  readonly wizardAbierto = signal(false);

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
        this.cargarTorneos(liga);
      },
      error: () => {
        this.error.set('No se pudo cargar la liga.');
        this.loading.set(false);
      }
    });
  }

  private cargarTorneos(liga: League): void {
    // Se piden los del juego y se filtra por liga: el listado ya viene
    // con la trazabilidad juego→liga→temporada→torneo resuelta.
    this.tournamentsService.listar(liga.juegoId).subscribe({
      next: (torneos) => this.torneos.set(torneos.filter((t) => t.ligaId === this.ligaId)),
      error: () => this.torneos.set([])
    });
  }

  torneoCreado(torneo: Torneo): void {
    this.wizardAbierto.set(false);
    const liga = this.league();
    if (liga) {
      this.cargarTorneos(liga);
    }
  }

  private cargarTemporadas(): void {
    this.leaguesService.listSeasons(this.ligaId).subscribe({
      next: (temporadas) => this.seasons.set(temporadas),
      error: () => this.seasonError.set('No se pudieron cargar las temporadas.')
    });
  }

  eliminarLiga(): void {
    this.eliminando.set(true);
    this.errorEliminar.set(null);
    this.leaguesService.delete(this.ligaId).subscribe({
      next: () => this.router.navigate(['/leagues']),
      error: (err) => {
        this.eliminando.set(false);
        this.confirmandoEliminar.set(false);
        this.errorEliminar.set(err?.error?.message ?? 'No se pudo eliminar la liga.');
      }
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
