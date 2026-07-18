import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { Transferencia } from '../../../../models/transferencia.model';
import { TransfersService } from '../../services/transfers.service';

/**
 * Seguimiento de las solicitudes de transferencia enviadas (RF-12).
 */
@Component({
  selector: 'app-transfer-list',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './transfer-list.component.html',
  styleUrl: './transfer-list.component.scss'
})
export class TransferListComponent implements OnInit {
  private readonly transfersService = inject(TransfersService);

  readonly enviadas = signal<Transferencia[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
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
}
