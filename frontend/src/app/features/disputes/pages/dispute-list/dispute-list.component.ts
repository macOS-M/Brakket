import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { MiDisputa } from '../../../../models/disputa.model';
import { DisputesService } from '../../services/disputes.service';

/**
 * Vista panorámica "Mis disputas" (mejora fuera de RF, no numerada en el
 * ERS): junta las disputas de TODOS los torneos donde el usuario tiene
 * algo que ver (las levantó, organiza el torneo, es árbitro asignado, o
 * comisionado de la liga). El detalle de cada una sigue viviendo dentro
 * del bracket del torneo (RF-30/31/32/33); esta pantalla solo evita
 * tener que entrar torneo por torneo a buscar cuál tiene algo pendiente.
 */
@Component({
  selector: 'app-dispute-list',
  standalone: true,
  imports: [StatusBadgeComponent],
  templateUrl: './dispute-list.component.html',
  styleUrl: './dispute-list.component.scss'
})
export class DisputeListComponent {
  private readonly disputesService = inject(DisputesService);
  private readonly router = inject(Router);

  readonly disputas = signal<MiDisputa[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    this.disputesService.misDisputas().subscribe({
      next: (datos) => {
        this.cargando.set(false);
        this.disputas.set(datos);
      },
      error: (err) => {
        this.cargando.set(false);
        this.error.set(err?.error?.message ?? 'No se pudieron cargar tus disputas.');
      }
    });
  }

  /** Lleva directo al bracket del torneo; ahí se busca la partida y su panel de disputa. */
  irAlTorneo(disputa: MiDisputa): void {
    this.router.navigate(['/tournaments', disputa.torneoId]);
  }

  formatearFecha(iso: string): string {
    return new Date(iso).toLocaleString('es-CR', {
      day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }
}
