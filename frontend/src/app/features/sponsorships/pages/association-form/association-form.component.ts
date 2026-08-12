import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { FechaInputComponent } from '../../../../shared/components/fecha-input/fecha-input.component';
import { PatrociniosService } from '../../services/patrocinios.service';
import { SponsorshipsService } from '../../services/sponsorships.service';
import { LeaguesService } from '../../../leagues/services/leagues.service';
import { TournamentsService } from '../../../tournaments/services/tournaments.service';
import { AlcancePatrocinio, NIVELES_PATROCINIO } from '../../../../models/patrocinio.model';
import { Patrocinador } from '../../../../models/patrocinador.model';
import { League, Season } from '../../../../models/league.model';
import { Torneo } from '../../../../models/tournament.model';

@Component({
  selector: 'app-association-form',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, FechaInputComponent],
  templateUrl: './association-form.component.html',
  styleUrl: './association-form.component.scss'
})
export class AssociationFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly patrociniosService = inject(PatrociniosService);
  private readonly sponsorshipsService = inject(SponsorshipsService);
  private readonly leaguesService = inject(LeaguesService);
  private readonly tournamentsService = inject(TournamentsService);
  private readonly router = inject(Router);

  readonly niveles = NIVELES_PATROCINIO;
  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);

  readonly patrocinadores = signal<Patrocinador[]>([]);
  readonly ligas = signal<League[]>([]);
  readonly temporadas = signal<Season[]>([]);
  readonly torneos = signal<Torneo[]>([]);

  readonly form = this.fb.nonNullable.group({
    patrocinadorId: [null as number | null, Validators.required],
    alcance: ['LIGA' as AlcancePatrocinio, Validators.required],
    ligaId: [null as number | null],
    temporadaLigaId: [null as number | null],
    temporadaId: [null as number | null],
    torneoId: [null as number | null],
    nivel: ['ORO', Validators.required],
    condiciones: [''],
    fechaInicio: ['', Validators.required],
    fechaFin: ['', Validators.required]
  });

  ngOnInit(): void {
    this.sponsorshipsService.listar().subscribe((data) => {
      this.patrocinadores.set(data.filter((p) => p.estado === 'ACTIVO'));
    });

    this.leaguesService.list().subscribe((data) => this.ligas.set(data));
    this.tournamentsService.listar().subscribe((data) => this.torneos.set(data));

    // Al elegir la liga origen para el alcance TEMPORADA, cargamos sus temporadas.
    this.form.get('temporadaLigaId')!.valueChanges.subscribe((ligaId) => {
      this.form.patchValue({ temporadaId: null });
      this.temporadas.set([]);
      if (ligaId) {
        this.leaguesService.listSeasons(ligaId).subscribe((data) => this.temporadas.set(data));
      }
    });

    // Al cambiar el alcance, limpiamos los selectores de los otros alcances
    // para no enviar accidentalmente mas de un id al backend.
    this.form.get('alcance')!.valueChanges.subscribe(() => {
      this.form.patchValue({
        ligaId: null,
        temporadaLigaId: null,
        temporadaId: null,
        torneoId: null
      });
      this.temporadas.set([]);
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valores = this.form.getRawValue();

    if (valores.alcance === 'TEMPORADA' && !valores.temporadaId) {
      this.error.set('Selecciona una temporada.');
      return;
    }

    this.guardando.set(true);
    this.error.set(null);

    this.patrociniosService.crear({
      patrocinadorId: valores.patrocinadorId!,
      ligaId: valores.alcance === 'LIGA' ? valores.ligaId : null,
      temporadaId: valores.alcance === 'TEMPORADA' ? valores.temporadaId : null,
      torneoId: valores.alcance === 'TORNEO' ? valores.torneoId : null,
      nivel: valores.nivel,
      condiciones: valores.condiciones || null,
      fechaInicio: valores.fechaInicio,
      fechaFin: valores.fechaFin
    }).subscribe({
      next: () => this.router.navigate(['/sponsorships/asociaciones']),
      error: (err) => {
        this.guardando.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo guardar la asociación.');
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/sponsorships/asociaciones']);
  }
}
