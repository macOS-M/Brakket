import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';

import { CrearTorneoRequest, Torneo } from '../../../../models/tournament.model';
import { League, Season } from '../../../../models/league.model';
import { TournamentsService } from '../../services/tournaments.service';
import { LeaguesService } from '../../../leagues/services/leagues.service';
import { CompetitiveProfileService } from '../../../../core/services/competitive-profile.service';
import { AuthService } from '../../../../core/services/auth.service';

/**
 * Wizard de creación de torneo (RF-24), 3 pasos como la referencia
 * Challenger Mode: General → Equipos → Fecha. El torneo puede ser
 * comunitario (solo el juego) u hospedarse en una temporada de una liga
 * del propio usuario. El perfil competitivo del juego, si existe, acota
 * los tamaños de equipo; sin perfil valen los estándares 1v1…5v5.
 */
@Component({
  selector: 'app-tournament-wizard',
  standalone: true,
  imports: [],
  templateUrl: './tournament-wizard.component.html',
  styleUrl: './tournament-wizard.component.scss'
})
export class TournamentWizardComponent implements OnInit {
  private readonly tournamentsService = inject(TournamentsService);
  private readonly leaguesService = inject(LeaguesService);
  private readonly perfilService = inject(CompetitiveProfileService);
  private readonly auth = inject(AuthService);

  readonly juegoId = input.required<number>();
  readonly juegoNombre = input.required<string>();
  readonly juegoImagen = input<string | null>(null);

  readonly cerrado = output<void>();
  readonly creado = output<Torneo>();

  readonly paso = signal<1 | 2 | 3>(1);
  readonly pasos = ['General', 'Equipos', 'Fecha'];

  // Paso 1 — General
  readonly nombre = signal('');
  readonly descripcion = signal('');
  readonly publico = signal(true);
  readonly ligaId = signal<number | null>(null);
  readonly temporadaId = signal<number | null>(null);

  // Paso 2 — Equipos
  readonly formato = signal('Eliminación directa');
  readonly tamano = signal(5);
  readonly cupo = signal(8);

  // Paso 3 — Fecha
  readonly fechaInicio = signal('');

  readonly misLigas = signal<League[]>([]);
  readonly temporadas = signal<Season[]>([]);
  readonly formatos = signal<string[]>([
    'Eliminación directa',
    'Doble eliminación',
    'Round robin',
    'Fase de grupos y eliminación',
    'Suizo'
  ]);
  readonly tamanosPermitidos = signal<number[]>([1, 2, 3, 4, 5]);
  readonly cupos = [2, 4, 8, 16, 32, 64];

  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);

  /** El paso actual está completo y se puede avanzar. */
  readonly pasoValido = computed(() => {
    switch (this.paso()) {
      case 1:
        return this.nombre().trim().length > 0
          && (this.ligaId() === null || this.temporadaId() !== null);
      case 2:
        return !!this.formato() && this.tamano() > 0 && this.cupo() >= 2;
      case 3:
        return !!this.fechaInicio() && new Date(this.fechaInicio()) > new Date();
    }
  });

  ngOnInit(): void {
    const usuarioId = Number(this.auth.usuario()?.id);
    this.leaguesService.list().subscribe({
      next: (ligas) => this.misLigas.set(
        ligas.filter((l) => l.comisionadoId === usuarioId && l.juegoId === this.juegoId())),
      error: () => this.misLigas.set([])
    });

    this.perfilService.listarFormatos().subscribe({
      next: (catalogo) => {
        if (catalogo.length > 0) {
          this.formatos.set(catalogo.map((f) => f.nombre));
          if (!catalogo.some((f) => f.nombre === this.formato())) {
            this.formato.set(catalogo[0].nombre);
          }
        }
      },
      error: () => undefined
    });

    // Perfil competitivo como curaduría opcional: si existe, acota tamaños.
    this.perfilService.obtenerPorJuego(this.juegoId()).subscribe({
      next: (perfil) => {
        if (perfil?.activo) {
          const permitidos: number[] = [];
          for (let n = perfil.plantillaMinima; n <= Math.min(perfil.plantillaMaxima, 10); n++) {
            permitidos.push(n);
          }
          if (permitidos.length > 0) {
            this.tamanosPermitidos.set(permitidos);
            this.tamano.set(permitidos[permitidos.length - 1]);
          }
        }
      },
      error: () => undefined
    });
  }

  elegirLiga(valor: string): void {
    const ligaId = valor ? Number(valor) : null;
    this.ligaId.set(ligaId);
    this.temporadaId.set(null);
    this.temporadas.set([]);
    if (ligaId) {
      this.leaguesService.listSeasons(ligaId).subscribe({
        next: (temporadas) => this.temporadas.set(temporadas),
        error: () => this.temporadas.set([])
      });
    }
  }

  siguiente(): void {
    if (!this.pasoValido()) {
      return;
    }
    this.paso.update((p) => (p < 3 ? ((p + 1) as 2 | 3) : p));
  }

  volver(): void {
    this.paso.update((p) => (p > 1 ? ((p - 1) as 1 | 2) : p));
  }

  crear(): void {
    if (!this.pasoValido() || this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.error.set(null);

    const request: CrearTorneoRequest = {
      nombre: this.nombre().trim(),
      juegoId: this.juegoId(),
      temporadaId: this.temporadaId(),
      formato: this.formato(),
      tamanoEquipo: this.tamano(),
      maxEquipos: this.cupo(),
      fechaInicio: this.fechaInicio(),
      publico: this.publico(),
      descripcion: this.descripcion().trim() || null
    };

    this.tournamentsService.crear(request).subscribe({
      next: (torneo) => this.creado.emit(torneo),
      error: (err) => {
        this.guardando.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo crear el torneo.');
      }
    });
  }

  cerrar(): void {
    if (!this.guardando()) {
      this.cerrado.emit();
    }
  }
}
