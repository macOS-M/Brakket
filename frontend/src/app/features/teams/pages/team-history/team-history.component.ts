import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { HistorialEquipo, MovimientoPlantilla } from '../../../../models/historial-equipo.model';
import { TeamsService } from '../../services/teams.service';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-team-history',
  standalone: true,
  imports: [DatePipe, RouterLink, FormsModule, EmptyStateComponent],
  templateUrl: './team-history.component.html',
  styleUrl: './team-history.component.scss'
})
export class TeamHistoryComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly teamsService = inject(TeamsService);

  equipoId = 0;

  readonly historial = signal<HistorialEquipo | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly desde = signal<string>('');
  readonly hasta = signal<string>('');

  ngOnInit(): void {
    const idParam = Number(this.route.snapshot.paramMap.get('equipoId'));
    if (!Number.isInteger(idParam) || idParam <= 0) {
      this.error.set('El identificador del equipo no es válido.');
      this.cargando.set(false);
      return;
    }
    this.equipoId = idParam;
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.teamsService
      .obtenerHistorial(this.equipoId, this.desde() || undefined, this.hasta() || undefined)
      .subscribe({
        next: (historial) => {
          this.historial.set(historial);
          this.cargando.set(false);
        },
        error: (err) => {
          this.error.set(
            err?.status === 404
              ? 'Equipo no encontrado.'
              : (err?.error?.message ?? 'No se pudo cargar el historial.')
          );
          this.cargando.set(false);
        }
      });
  }

  limpiarFiltro(): void {
    this.desde.set('');
    this.hasta.set('');
    this.cargar();
  }

  etiquetaTipo(mov: MovimientoPlantilla): string {
    switch (mov.tipo) {
      case 'ALTA': return 'Alta';
      case 'BAJA': return 'Baja';
      case 'TRANSFERENCIA': return 'Transferencia';
      default: return mov.tipo;
    }
  }
}
