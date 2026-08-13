import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { FechaInputComponent } from '../../../../shared/components/fecha-input/fecha-input.component';
import { fechaCorta } from '../../../../shared/utils/formato-fecha';
import { PatrociniosService } from '../../services/patrocinios.service';
import { SponsorshipsService } from '../../services/sponsorships.service';
import { LeaguesService } from '../../../leagues/services/leagues.service';
import { TournamentsService } from '../../../tournaments/services/tournaments.service';
import { AlcancePatrocinio } from '../../../../models/patrocinio.model';
import { Patrocinador } from '../../../../models/patrocinador.model';
import { League } from '../../../../models/league.model';
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

  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);

  readonly patrocinadores = signal<Patrocinador[]>([]);
  readonly ligas = signal<League[]>([]);
  readonly torneos = signal<Torneo[]>([]);

  readonly form = this.fb.nonNullable.group({
    patrocinadorId: [null as number | null, Validators.required],
    alcance: ['LIGA' as AlcancePatrocinio, Validators.required],
    ligaId: [null as number | null],
    torneoId: [null as number | null],
    condiciones: [''],
    fechaInicio: ['', Validators.required],
    fechaFin: ['', Validators.required]
  });

  ngOnInit(): void {
    this.sponsorshipsService.listar().subscribe((data) => {
      this.patrocinadores.set(data.filter((p) => p.estado === 'ACTIVO'));
    });

    // Solo ligas activas: una liga inactiva tampoco debería recibir patrocinios
    // nuevos (mismo criterio que el backend, que ahora valida esto también).
    this.leaguesService.list().subscribe((data) => this.ligas.set(data.filter((l) => l.activo)));

    // No tiene sentido patrocinar un torneo que ya terminó o se canceló: se
    // filtra acá para que ni siquiera aparezca como opción, y el backend
    // también lo rechaza si alguien lo intenta saltándose el formulario.
    this.tournamentsService.listar().subscribe((data) =>
      this.torneos.set(data.filter((t) => t.estado !== 'FINALIZADO' && t.estado !== 'CANCELADO')));

    // Al cambiar el alcance, limpiamos el otro selector para no enviar
    // accidentalmente mas de un id al backend.
    this.form.get('alcance')!.valueChanges.subscribe(() => {
      this.form.patchValue({ ligaId: null, torneoId: null });
    });
  }

  /**
   * Ayuda junto a las fechas: el admin no debería tener que saber de memoria
   * el rango disponible de la liga/torneo elegido.
   *
   * <p>Liga no tiene ninguna columna de fecha en la base: el texto explica
   * que las fechas acá describen el contrato de patrocinio, no un calendario
   * de la liga misma. Torneo solo expone `fechaInicio`: `fecha_fin` en BD se
   * llena cuando el torneo realmente termina, no es una fecha planeada de
   * antemano. No hace falta advertir sobre torneos cerrados acá: ya no
   * aparecen en el dropdown.</p>
   */
  rangoDisponible(): string | null {
    const valores = this.form.getRawValue();
    switch (valores.alcance) {
      case 'TORNEO': {
        const torneo = this.torneos().find((t) => t.id === valores.torneoId);
        return torneo ? `El torneo comienza el ${fechaCorta(torneo.fechaInicio)}.` : null;
      }
      case 'LIGA':
        return 'Las ligas no tienen fecha propia — indica el período del contrato de patrocinio.';
      default:
        return null;
    }
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valores = this.form.getRawValue();

    this.guardando.set(true);
    this.error.set(null);

    this.patrociniosService.crear({
      patrocinadorId: valores.patrocinadorId!,
      ligaId: valores.alcance === 'LIGA' ? valores.ligaId : null,
      temporadaId: null,
      torneoId: valores.alcance === 'TORNEO' ? valores.torneoId : null,
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
