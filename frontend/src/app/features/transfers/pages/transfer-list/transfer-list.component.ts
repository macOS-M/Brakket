import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { Transferencia } from '../../../../models/transferencia.model';
import { colorDeNombre } from '../../../../shared/utils/cover';
import { TransfersService } from '../../services/transfers.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { RolEquipoPipe } from '../../../../shared/pipes/rol-equipo.pipe';

/**
 * Transferencias: bandeja de solicitudes pendientes por responder (RF-13)
 * y seguimiento de las solicitudes enviadas (RF-12).
 */
@Component({
  selector: 'app-transfer-list',
  standalone: true,
  imports: [RouterLink, DatePipe, PageHeaderComponent, EmptyStateComponent, StatusBadgeComponent, RolEquipoPipe],
  templateUrl: './transfer-list.component.html',
  styleUrl: './transfer-list.component.scss'
})
export class TransferListComponent implements OnInit {
  private readonly transfersService = inject(TransfersService);

  readonly enviadas = signal<Transferencia[]>([]);
  readonly pendientes = signal<Transferencia[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  /** Id de la solicitud cuya respuesta está en vuelo (deshabilita sus botones). */
  readonly respondiendo = signal<number | null>(null);
  readonly mensaje = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.transfersService.pendientes().subscribe({
      next: (pendientes) => {
        this.pendientes.set(pendientes);
        this.cargarEnviadas();
      },
      error: () => {
        this.error.set('No se pudieron cargar las solicitudes de transferencia.');
        this.cargando.set(false);
      }
    });
  }

  responder(transferencia: Transferencia, decision: 'ACEPTAR' | 'RECHAZAR'): void {
    if (this.respondiendo() !== null) {
      return;
    }
    this.respondiendo.set(transferencia.id);
    this.mensaje.set(null);
    this.error.set(null);
    this.transfersService.responder(transferencia.id, decision).subscribe({
      next: (actualizada) => {
        this.respondiendo.set(null);
        this.mensaje.set(
          actualizada.estado === 'APROBADA'
            ? `Transferencia completada: ${actualizada.jugadorNombre} ahora juega en ${actualizada.equipoDestinoNombre}.`
            : actualizada.estado === 'RECHAZADA'
              ? 'La solicitud fue rechazada y el proceso quedó cancelado.'
              : 'Respuesta registrada; falta la aprobación de la otra parte.'
        );
        this.cargar();
      },
      error: (err) => {
        this.respondiendo.set(null);
        this.error.set(err?.error?.message ?? 'No se pudo registrar la respuesta.');
      }
    });
  }

  /** Color estable para el monograma del jugador (mismo nombre → mismo color). */
  colorDe(nombre: string): string {
    return colorDeNombre(nombre);
  }

  etiquetaEstado(estado: string): string {
    switch (estado) {
      case 'APROBADA':
        return 'Aprobada';
      case 'RECHAZADA':
        return 'Rechazada';
      default:
        return 'Pendiente';
    }
  }

  etiquetaAprobacion(aprobacion: string): string {
    switch (aprobacion) {
      case 'ACEPTADA':
        return 'Aceptó';
      case 'RECHAZADA':
        return 'Rechazó';
      default:
        return 'Sin responder';
    }
  }

  private cargarEnviadas(): void {
    this.transfersService.enviadas().subscribe({
      next: (transferencias) => {
        this.enviadas.set(transferencias);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las solicitudes de transferencia.');
        this.cargando.set(false);
      }
    });
  }
}
